/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015, Maxim Roncacé <mproncace@lapis.blue>
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
package net.caseif.ttt.command.misc;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.CommandHandler;
import net.caseif.ttt.util.Constants.Color;
import net.caseif.ttt.util.helper.misc.MiscHelper;

import org.bukkit.command.CommandSender;

public class DefaultCommand extends CommandHandler {

    public DefaultCommand(CommandSender sender, String[] args) {
        super(sender, args, null);
    }

    @Override
    public void handle() {
        TTTCore.locale.getLocalizable("info.plugin.info").withPrefix(Color.SPECIAL)
                .withReplacements(TTTCore.getPlugin().getDescription().getVersion()
                                + " \"" + TTTCore.getCodename() + "\"",
                        MiscHelper.prettyList(TTTCore.getPlugin().getDescription().getAuthors()))
                .sendTo(sender);
        TTTCore.locale.getLocalizable("info.command.usage.help").withPrefix(Color.INFO).sendTo(sender);
    }

}
