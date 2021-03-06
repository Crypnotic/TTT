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

package net.caseif.ttt.command.handler.round;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.handler.CommandHandler;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.constant.MetadataKey;
import net.caseif.ttt.util.constant.Stage;

import com.google.common.base.Optional;
import net.caseif.flint.arena.Arena;
import org.bukkit.command.CommandSender;

public class EndCommand extends CommandHandler {

    public EndCommand(CommandSender sender, String[] args) {
        super(sender, args);
    }

    @Override
    public void handle() {
        handle(false);
    }

    protected void handle(boolean force) {
        String arenaName = args[1];
        Optional<Arena> arena = TTTCore.mg.getArena(arenaName);
        if (!arena.isPresent()) {
            TTTCore.locale.getLocalizable("error.arena.dne").withPrefix(Color.ERROR)
                    .withReplacements(Color.ARENA + arenaName + Color.ERROR).sendTo(sender);
            return;
        }

        if (!arena.get().getRound().isPresent()
                || arena.get().getRound().get().getLifecycleStage() == Stage.WAITING) {
            TTTCore.locale.getLocalizable("error.arena.no-round").withPrefix(Color.ERROR)
                    .withReplacements(Color.ARENA + arenaName + Color.ERROR).sendTo(sender);
            return;
        }

        if (args.length > 2) {
            if (args[2].equalsIgnoreCase("t")) {
                arena.get().getRound().get().getMetadata().set(MetadataKey.Round.TRAITOR_VICTORY, true);
            } else if (!args[2].equalsIgnoreCase("i")) {
                printInvalidArgsError();
                return;
            }
        }

        arena.get().getRound().get().setLifecycleStage(Stage.ROUND_OVER, true);
        if (force) {
            arena.get().getRound().get().setTime(Stage.ROUND_OVER.getDuration() + 1);
        }
    }

}
