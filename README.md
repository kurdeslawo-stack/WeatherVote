# WeatherVote

Lightweight weather and time voting plugin for Spigot/Paper servers.

## Features

- `/vot Day`, `/vot Night`, `/vot Sun`, `/vot Rain`, `/vot Storm`
- `/vot Yes` and `/vot No` for voting
- clickable **YES / NO** chat buttons when a vote starts
- automatic YES vote for the player who starts a vote
- configurable vote duration, minimum players and category cooldowns
- optional BossBar, ActionBar and interactive chat controls
- configurable target worlds
- Polish and English language files
- no runtime dependencies
- legacy `voting.vote.*` permissions kept for existing installations

## Language

Set the language in `config.yml`:

```yml
language: pl
```

Supported values:

- `pl` -> `lang_pl.yml`
- `eng` or `en` -> `lang_eng.yml`

All player-facing messages can be edited directly in the language files.

## Interactive chat voting

Enabled by default:

```yml
Visual:
  InteractiveChat:
    Active: true
```

When a vote starts, players can click the green **I'M FOR / JESTEM ZA** or red **I'M AGAINST / JESTEM PRZECIW** button instead of typing `/vot yes` or `/vot no`.

## Commands

```text
/vot <Day|Night|Sun|Rain|Storm|Yes|No>
/weathervote <...>
/wv <...>
/vot reload
```

## Permissions

```text
weathervote.vote.start   default: true
weathervote.vote.place   default: true
weathervote.admin        default: op
```

## Compatibility target

The public-release branch is intentionally compiled against the Spigot 1.13.2 API with Java 8 bytecode and avoids modern-only APIs. The goal is one JAR for older and current Spigot/Paper versions. Compatibility still needs runtime smoke tests before publishing the final release.

## Build

```bash
mvn clean package
```

The resulting JAR is created in `target/`.
