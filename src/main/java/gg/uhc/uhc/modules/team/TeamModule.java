package gg.uhc.uhc.modules.team;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gg.uhc.uhc.modules.Module;
import gg.uhc.uhc.modules.team.prefixes.Prefix;
import gg.uhc.uhc.modules.team.prefixes.PrefixColourPredicateConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeamModule extends Module {

    protected final Scoreboard scoreboard;

    protected Map<String, Team> teams;

    public TeamModule() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        this.icon.setType(Material.WOOL);
        this.icon.setDurability((short) 3);
        this.icon.setDisplayName(ChatColor.GREEN + "Team Manager");
        this.icon.setWeight(100);
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public Map<String, Team> getTeams() {
        return teams;
    }

    public Optional<Team> findFirstEmptyTeam() {
        return Iterables.tryFind(teams.values(), Predicates.not(FunctionalUtil.TEAMS_WITH_PLAYERS));
    }

    @Override
    public void initialize(ConfigurationSection section) throws InvalidConfigurationException {
        if (!section.contains("removed team combos")) {
            section.set("removed team combos", Lists.newArrayList(
                    "RESET",
                    "STRIKETHROUGH",
                    "MAGIC",
                    "BLACK",
                    "WHITE",
                    "=GRAY+ITALIC" // spectator styling in tab
            ));
        }

        Predicate<Prefix> isFiltered;
        try {
            List<String> removedCombos = section.getStringList("removed team combos");
            List<Predicate<Prefix>> temp = Lists.newArrayListWithCapacity(removedCombos.size());

            PrefixColourPredicateConverter converter = new PrefixColourPredicateConverter();
            for (String combo : removedCombos) {
                temp.add(converter.convert(combo));
            }

            isFiltered = Predicates.or(temp);
        } catch (Exception ex) {
            ex.printStackTrace();
            plugin.getLogger().severe("Failed to parse filtered team combos, allowing all combos to be used instead");
            isFiltered = Predicates.alwaysFalse();
        }

        setupTeams(Predicates.not(isFiltered));
        this.icon.setLore("Managing " + teams.size() + " teams", "Not disableable");
    }

    protected void setupTeams(Predicate<Prefix> allowedPrefixes) {
        ImmutableMap.Builder<String, Team> teamBuilder = ImmutableMap.builder();

        // separate colours + formatting codes
        Set<ChatColor> colours = Sets.newHashSet();
        Set<ChatColor> formatting = Sets.newHashSet();

        for (ChatColor colour : ChatColor.values()) {
            if (colour.isColor()) {
                colours.add(colour);
            } else {
                formatting.add(colour);
            }
        }

        Set<Set<ChatColor>> formattingCombinations = Sets.powerSet(formatting);

        int teamId = 1;
        int totalId = formattingCombinations.size() * colours.size();
        Joiner joiner = Joiner.on("");
        String reset = ChatColor.RESET.toString();

        for (Set<ChatColor> combination : formattingCombinations) {
            for (ChatColor color : colours) {
                if (!allowedPrefixes.apply(new Prefix(color, combination))) continue;

                String prefix = color + joiner.join(combination);
                String teamName = "UHC-" + teamId;

                Team team = scoreboard.getTeam(teamName);

                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                }

                team.setDisplayName("UHC Team " + teamId);
                team.setPrefix(prefix);
                team.setSuffix(reset);

                teamBuilder.put(teamName, team);
                teamId++;
            }
        }

        // unregister extra ids
        for (int i = teamId; i <= totalId; i++) {
            Team team = scoreboard.getTeam("UHC-" + i);

            if (team != null) {
                team.unregister();
            }
        }

        teams = teamBuilder.build();
    }
}
