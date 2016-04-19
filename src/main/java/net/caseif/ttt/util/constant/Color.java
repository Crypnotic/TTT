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

package net.caseif.ttt.util.constant;

import net.caseif.ttt.TTTBootstrap;
import net.caseif.ttt.TTTCore;

import org.bukkit.ChatColor;

// message colors
public class Color {

    public static final String INFO = (TTTBootstrap.STEEL && TTTCore.HALLOWEEN
            ? ChatColor.GOLD
            : ChatColor.DARK_AQUA).toString();
    public static final String ERROR = ChatColor.RED.toString();

    public static final String FADED = ChatColor.GRAY.toString();
    public static final String FLAIR = (TTTBootstrap.STEEL && TTTCore.HALLOWEEN
            ? ChatColor.DARK_AQUA
            : ChatColor.GOLD).toString();
    public static final String LABEL = ChatColor.GREEN.toString();
    public static final String SPECIAL = ChatColor.LIGHT_PURPLE.toString();

    public static final String DETECTIVE = ChatColor.BLUE.toString();
    public static final String INNOCENT = ChatColor.DARK_GREEN.toString();
    public static final String TRAITOR = ChatColor.DARK_RED.toString();

    private Color() {
    }

}
