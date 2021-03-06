/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2016, Max Roncace <me@caseif.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caseif.ttt.listeners.minigame;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.scoreboard.ScoreboardManager;
import net.caseif.ttt.util.RoundRestartDaemon;
import net.caseif.ttt.util.config.ConfigKey;
import net.caseif.ttt.util.config.OperatingMode;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.constant.MetadataKey;
import net.caseif.ttt.util.constant.Role;
import net.caseif.ttt.util.constant.Stage;
import net.caseif.ttt.util.helper.data.TelemetryStorageHelper;
import net.caseif.ttt.util.helper.gamemode.ArenaHelper;
import net.caseif.ttt.util.helper.gamemode.RoleHelper;
import net.caseif.ttt.util.helper.gamemode.RoundHelper;

import com.google.common.eventbus.Subscribe;
import net.caseif.flint.challenger.Challenger;
import net.caseif.flint.challenger.Team;
import net.caseif.flint.config.ConfigNode;
import net.caseif.flint.event.round.RoundChangeLifecycleStageEvent;
import net.caseif.flint.event.round.RoundEndEvent;
import net.caseif.flint.event.round.RoundTimerTickEvent;
import net.caseif.flint.round.Round;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Listener for round events.
 */
public class RoundListener {

    @Subscribe
    public void onRoundChangeLifecycleStage(RoundChangeLifecycleStageEvent event) {
        if (event.getStageAfter() == Stage.PREPARING) {
            RoundHelper.broadcast(event.getRound(), TTTCore.locale.getLocalizable("info.global.round.event.starting")
                    .withPrefix(Color.INFO));
        } else if (event.getStageAfter() == Stage.PLAYING) {
            RoundHelper.startRound(event.getRound());
            if (TTTCore.config.get(ConfigKey.ENABLE_TELEMETRY)) {
                event.getRound().getMetadata().set(MetadataKey.Round.ROUND_PLAYER_COUNT,
                        event.getRound().getChallengers().size());
            }
        } else if (event.getStageAfter() == Stage.ROUND_OVER) {
            RoundHelper.closeRound(event.getRound(), event.getStageBefore() == Stage.PLAYING);
            if (ArenaHelper.shouldArenaCycle(event.getRound().getArena())) {
                RoundHelper.broadcast(event.getRound(),
                        TTTCore.locale.getLocalizable("info.global.arena.status.map-change")
                                .withPrefix(Color.INFO));
            }

            if (TTTCore.config.get(ConfigKey.ENABLE_TELEMETRY)) {
                if (!event.getRound().getMetadata().has(MetadataKey.Round.ROUND_DURATION)) {
                    event.getRound().getMetadata()
                            .set(MetadataKey.Round.ROUND_DURATION, event.getStageBefore().getDuration());
                }
                if (!event.getRound().getMetadata().has(MetadataKey.Round.ROUND_RESULT)) {
                    event.getRound().getMetadata().set(MetadataKey.Round.ROUND_RESULT, 2);
                }
                TelemetryStorageHelper.pushRound(event.getRound());
            }
        }
    }

    @SuppressWarnings({"deprecation"})
    @Subscribe
    public void onRoundTick(RoundTimerTickEvent event) {
        Round r = event.getRound();

        r.getMetadata().<ScoreboardManager>get(MetadataKey.Round.SCOREBOARD_MANAGER).get().updateTitle();

        if (r.getLifecycleStage() != Stage.WAITING) {
            if (event.getRound().getLifecycleStage() == Stage.PLAYING) {
                // check if game is over
                boolean iLeft = false;
                boolean tLeft = false;
                for (Challenger ch : event.getRound().getChallengers()) {
                    if (!(tLeft && iLeft)) {
                        if (!ch.isSpectating()) {
                            if (RoleHelper.isTraitor(ch)) {
                                tLeft = true;
                            } else {
                                iLeft = true;
                            }
                        }
                    } else {
                        break;
                    }

                    // manage DNA scanners
                    Player tracker = TTTCore.getPlugin().getServer().getPlayer(ch.getName());
                    if (ch.getMetadata().has(Role.DETECTIVE) && ch.getMetadata().has("tracking")) {
                        // update every n secconds
                        if (ch.getRound().getTime() % TTTCore.config.get(ConfigKey.SCANNER_CHARGE_TIME) == 0) {
                            Player killer = TTTCore.getPlugin().getServer()
                                    .getPlayer(ch.getMetadata().<UUID>get("tracking").get());
                            if (killer != null
                                    && TTTCore.mg.getChallenger(killer.getUniqueId()).isPresent()) {
                                tracker.setCompassTarget(killer.getLocation());
                            } else {
                                TTTCore.locale.getLocalizable("error.round.trackee-left")
                                        .withPrefix(Color.ERROR).sendTo(tracker);
                                ch.getMetadata().remove("tracking");
                                tracker.setCompassTarget(Bukkit.getWorlds().get(1).getSpawnLocation());
                            }
                        }
                    } else { // just give it random coordinates
                        tracker.setCompassTarget(
                                new Location(
                                        tracker.getWorld(),
                                        tracker.getLocation().getX() + (Math.random() * 100 - 50),
                                        tracker.getLocation().getY(),
                                        tracker.getLocation().getZ() + (Math.random() * 100 - 50)
                                )
                        );
                    }
                }

                if (!(tLeft && iLeft)) {
                    if (tLeft) {
                        event.getRound().getMetadata().set(MetadataKey.Round.TRAITOR_VICTORY, true);
                    }

                    if (TTTCore.config.get(ConfigKey.ENABLE_TELEMETRY)) {
                        event.getRound().getMetadata().set(MetadataKey.Round.ROUND_RESULT, (byte) (tLeft ? 1 : 0));
                        event.getRound().getMetadata()
                                .set(MetadataKey.Round.ROUND_DURATION, (int) event.getRound().getTime());
                    }

                    event.getRound().setLifecycleStage(Stage.ROUND_OVER, true);
                }
            }
        }
    }

    @Subscribe
    public void onRoundEnd(RoundEndEvent event) {
        if (event.getRound().getLifecycleStage() != Stage.ROUND_OVER) {
            RoundHelper.closeRound(event.getRound(), event.getRound().getLifecycleStage() == Stage.PLAYING);
        }

        for (Entity ent : Bukkit.getWorld(event.getRound().getArena().getWorld()).getEntities()) {
            if (ent.getType() == EntityType.ARROW) {
                ent.remove();
            }
        }

        event.getRound().getMetadata().<ScoreboardManager>get(MetadataKey.Round.SCOREBOARD_MANAGER).get()
                .uninitialize();

        if (event.isNatural()
                && (TTTCore.config.get(ConfigKey.OPERATING_MODE) == OperatingMode.CONTINUOUS
                || TTTCore.config.get(ConfigKey.OPERATING_MODE) == OperatingMode.DEDICATED)) {
            // restart the round
            new RoundRestartDaemon(event.getRound()).runTask(TTTCore.getPlugin());
        }
    }

    @Subscribe
    public void onStageChange(RoundChangeLifecycleStageEvent event) {
        if (event.getStageBefore() == Stage.PREPARING && event.getStageAfter() == Stage.WAITING) {
            for (Challenger ch : event.getRound().getChallengers()) {
                Bukkit.getPlayer(ch.getUniqueId())
                        .setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }

            for (Team team : event.getRound().getTeams()) {
                event.getRound().removeTeam(team);
            }

            event.getRound().getMetadata().<ScoreboardManager>get(MetadataKey.Round.SCOREBOARD_MANAGER).get()
                    .updateAllEntries();
        } else if (event.getStageAfter() == Stage.ROUND_OVER) {
            event.getRound().setConfigValue(ConfigNode.WITHHOLD_SPECTATOR_CHAT, false);
        }
    }

}
