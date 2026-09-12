package pl.slawcio.weathervote;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class TransitionEffects {

    private final WeatherVotePlugin plugin;

    public TransitionEffects(WeatherVotePlugin plugin) {
        this.plugin = plugin;
    }

    public void play(final World world, final VotingUnit unit) {
        if (!plugin.getConfig().getBoolean("Visual.Transitions.Active", true)) {
            unit.apply(world);
            return;
        }

        announce(world, unit);

        if (unit == VotingUnit.DAY || unit == VotingUnit.NIGHT) {
            animateTime(world, unit);
            return;
        }

        animateWeather(world, unit);
    }

    private void animateTime(final World world, final VotingUnit unit) {
        final int duration = clampDuration(plugin.getConfig().getInt("Visual.Transitions.DurationTicks", 60));
        final long start = normalizeTime(world.getTime());
        final long target = unit == VotingUnit.DAY ? 1000L : 13000L;
        final long delta = shortestTimeDelta(start, target);

        new BukkitRunnable() {
            private int elapsed;

            @Override
            public void run() {
                elapsed++;
                double progress = Math.min(1.0D, (double) elapsed / (double) duration);
                double eased = 0.5D - (Math.cos(Math.PI * progress) / 2.0D);
                long animatedTime = normalizeTime(start + Math.round(delta * eased));
                world.setTime(animatedTime);

                if (particlesEnabled() && (elapsed == 1 || elapsed % 10 == 0)) {
                    pulseParticles(world, unit);
                }

                if (elapsed >= duration) {
                    unit.apply(world);
                    finish(world, unit);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void animateWeather(final World world, final VotingUnit unit) {
        final int duration = clampDuration(plugin.getConfig().getInt("Visual.Transitions.DurationTicks", 60));
        final int changeAt = Math.max(8, duration / 3);

        if (particlesEnabled()) {
            pulseParticles(world, unit);
        }

        new BukkitRunnable() {
            private int elapsed;
            private boolean applied;

            @Override
            public void run() {
                elapsed++;

                if (!applied && elapsed >= changeAt) {
                    unit.apply(world);
                    applied = true;
                    playImpactSound(world, unit);
                    if (particlesEnabled()) {
                        pulseParticles(world, unit);
                    }
                }

                if (particlesEnabled() && elapsed % 12 == 0) {
                    pulseParticles(world, unit);
                }

                if (elapsed >= duration) {
                    if (!applied) {
                        unit.apply(world);
                    }
                    finish(world, unit);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void announce(World world, VotingUnit unit) {
        String key = unit.getLanguageKey();
        String title = plugin.raw("VisualMessages.Transitions." + key + ".Title");
        String subtitle = plugin.raw("VisualMessages.Transitions." + key + ".Subtitle");

        for (Player player : world.getPlayers()) {
            if (plugin.getConfig().getBoolean("Visual.Transitions.Titles", true)) {
                player.sendTitle(title, subtitle, 10, 40, 10);
            }
            if (plugin.getConfig().getBoolean("Visual.Transitions.Sounds", true)) {
                playStartSound(player, unit);
            }
        }
    }

    private void playStartSound(Player player, VotingUnit unit) {
        switch (unit) {
            case DAY:
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.55F, 1.25F);
                break;
            case NIGHT:
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.45F, 0.65F);
                break;
            case SUN:
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5F, 1.35F);
                break;
            case RAIN:
                player.playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.55F, 1.0F);
                break;
            case STORM:
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7F, 1.05F);
                break;
            default:
                break;
        }
    }

    private void playImpactSound(World world, VotingUnit unit) {
        if (!plugin.getConfig().getBoolean("Visual.Transitions.Sounds", true)) {
            return;
        }

        for (Player player : world.getPlayers()) {
            switch (unit) {
                case SUN:
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.45F, 1.5F);
                    break;
                case RAIN:
                    player.playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.7F, 0.9F);
                    break;
                case STORM:
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9F, 0.85F);
                    break;
                default:
                    break;
            }
        }
    }

    private void finish(World world, VotingUnit unit) {
        if (!plugin.getConfig().getBoolean("Visual.Transitions.Sounds", true)) {
            return;
        }

        if (unit != VotingUnit.DAY && unit != VotingUnit.NIGHT) {
            return;
        }

        for (Player player : world.getPlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35F,
                    unit == VotingUnit.DAY ? 1.6F : 0.8F);
        }
    }

    private void pulseParticles(World world, VotingUnit unit) {
        for (Player player : world.getPlayers()) {
            Location location = player.getLocation().clone().add(0.0D, 1.0D, 0.0D);

            switch (unit) {
                case DAY:
                    player.spawnParticle(Particle.END_ROD, location, 8, 0.65D, 0.85D, 0.65D, 0.015D);
                    break;
                case NIGHT:
                    player.spawnParticle(Particle.PORTAL, location, 14, 0.8D, 0.75D, 0.8D, 0.04D);
                    break;
                case SUN:
                    player.spawnParticle(Particle.VILLAGER_HAPPY, location, 8, 0.8D, 0.7D, 0.8D, 0.02D);
                    break;
                case RAIN:
                    player.spawnParticle(Particle.WATER_DROP, location.clone().add(0.0D, 0.8D, 0.0D), 18,
                            1.1D, 0.4D, 1.1D, 0.02D);
                    break;
                case STORM:
                    player.spawnParticle(Particle.SMOKE_LARGE, location, 10, 0.9D, 0.55D, 0.9D, 0.02D);
                    break;
                default:
                    break;
            }
        }
    }

    private boolean particlesEnabled() {
        return plugin.getConfig().getBoolean("Visual.Transitions.Particles", true);
    }

    private int clampDuration(int duration) {
        return Math.max(20, Math.min(200, duration));
    }

    private long normalizeTime(long value) {
        long normalized = value % 24000L;
        return normalized < 0L ? normalized + 24000L : normalized;
    }

    private long shortestTimeDelta(long start, long target) {
        long delta = target - start;
        if (delta > 12000L) {
            delta -= 24000L;
        } else if (delta < -12000L) {
            delta += 24000L;
        }
        return delta;
    }
}
