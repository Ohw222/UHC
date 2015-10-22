/*
 * Project: UHC
 * Class: gg.uhc.uhc.modules.team.TeamRemoveCommand
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Graham Howden <graham_howden1 at yahoo.co.uk>.
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

package gg.uhc.uhc.modules.team;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import gg.uhc.flagcommands.commands.OptionCommand;
import gg.uhc.flagcommands.converters.OfflinePlayerConverter;
import gg.uhc.flagcommands.converters.TeamConverter;
import gg.uhc.flagcommands.joptsimple.OptionSet;
import gg.uhc.flagcommands.joptsimple.OptionSpec;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.Team;

import java.util.Set;

public class TeamRemoveCommand extends OptionCommand {

    protected static final String COMPLETE = ChatColor.AQUA + "Removed %d players, team is now: " + ChatColor.DARK_PURPLE + "%s";

    protected final OptionSpec<Team> teamSpec;
    protected final OptionSpec<OfflinePlayer> playersSpec;
    protected final OptionSpec<Void> removeAllSpec;

    public TeamRemoveCommand(TeamModule module) {
        teamSpec = parser
                .acceptsAll(ImmutableList.of("t", "team"), "Name of the team to remove players from")
                .withRequiredArg()
                .required()
                .withValuesConvertedBy(new TeamConverter(module.getScoreboard()));

        playersSpec = parser
                .nonOptions("List of player names to remove from the specified team")
                .withValuesConvertedBy(new OfflinePlayerConverter());

        removeAllSpec = parser
                .acceptsAll(ImmutableList.of("a", "all"), "Remove all players from the team");
    }

    @Override
    protected boolean runCommand(CommandSender sender, OptionSet options) {
        Team team = teamSpec.value(options);

        Set<OfflinePlayer> players;
        if (options.has(removeAllSpec)) {
            players = team.getPlayers();
        } else {
            players = Sets.intersection(team.getPlayers(), Sets.newHashSet(playersSpec.values(options)));
        }

        for (OfflinePlayer player : players) {
            team.removePlayer(player);
        }

        Set<OfflinePlayer> finalTeam = team.getPlayers();
        String members = finalTeam.size() == 0 ? ChatColor.DARK_GRAY + "No members" : Joiner.on(", ").join(Iterables.transform(team.getPlayers(), FunctionalUtil.PLAYER_NAME_FETCHER));

        sender.sendMessage(String.format(COMPLETE, players.size(), members));
        return true;
    }
}
