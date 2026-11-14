# Teste num Crucible de verdade

Kit usado para testar este Skript num servidor Crucible 1.7.10 real, no Java 21, com Et Futurum Requiem,
Netherlicious, NecroTempus e Kunftig, e com um bot conectado como jogador.

## Servidor

Em uma pasta `server/`:

| Arquivo | De onde |
|---|---|
| `crucible.jar` | Release `staging-cd1de3d` do Crucible: `https://github.com/CrucibleMC/Crucible/releases/download/staging-cd1de3d/Crucible-1.7.10-staging-cd1de3d-dev-283-server.jar` |
| `java9args.txt` | Mesma release |
| `mods/lwjgl3ify-3.1.0.jar` | Modrinth (`lwjgl3ify`). **Obrigatório no Java 9+**: sem ele o Forge quebra em `java.util.jar.Pack200` |
| `mods/+unimixins-all-1.7.10-0.3.1.jar` | Modrinth (`unimixins`). Exigido pelo lwjgl3ify, Et Futurum e Kunftig |
| `mods/etfuturum-2.6.2.jar` | Modrinth (`etfuturum`) |
| `mods/netherlicious-3.2.8.jar` | CurseForge (arquivo 4363437) |
| `mods/NecroTempus-v2.2.0.jar` | Release `v2.2.0` do CrucibleMC/NecroTempus |
| `mods/omniconfig-1.0.1-mc1.7.10.jar` | CurseForge (arquivo 4662703). Exigido pelo NecroTempus |
| `mods/kunftig-<commit>.jar` | `./gradlew build` no repositório do Kunftig |
| `plugins/Skript.jar`, `plugins/skript-reflect.jar` | Os dois forks deste projeto |

O Crucible baixa as bibliotecas na primeira execução. Três delas vêm de `github.com/juanmuscaria/maven/raw/...`; se essa
URL estiver bloqueada, baixe pelo `raw.githubusercontent.com` (mesmo caminho) para `libraries/<caminho maven>/` e crie
ao lado um `<arquivo>.jar.md5` com o MD5 do jar, senão o Crucible tenta baixar de novo.

`server.properties`: `online-mode=false` (para o bot) e `level-type=FLAT`. Para iniciar:

```
java -Xmx3G $(cat java9args.txt | tr '\n' ' ') -jar crucible.jar nogui
```

## Bot

```
cd bot && npm install && node bot.js Tester
```

O bot faz o handshake do FML (com a correção para a 1.7.10 aplicada pelo `postinstall`), registra no log chat (com
eventos de clique/hover), pacotes de plugin (NecroTempus, Kunftig), partículas, slots do inventário e spawns, e manda cada
linha nova de `bot/bot.in` como chat/comando. Comandos especiais: `!use` (clique direito com o item da mão), `!attack`
(ataca a última entidade que apareceu), `!look <yaw> <pitch>`.

## Scripts

Copie `scripts/*.sk` para `plugins/Skript/scripts/` e dê OP ao bot (`op Tester`).

* `realtest.sk` (console): `/rtaliases` (aliases do Et Futurum e Netherlicious), `/rtspawn`, `/rtcount`, `/rtsave` e
  `/rtload` (variáveis com itens e tipos de entidade de mod).
* `playertest.sk` (bot): `/pttitle`, `/ptbar`, `/ptchat`, `/ptgive`, `/ptspawn`, `/ptride`, `/ptsword` + `!attack`
  (evento de dano em item), `/ptrespawn`, `/ptsave` e `/ptload`.
* `kunftigtest.sk` (bot): `/ktpart`, `/ktdata` e `/ktdataload` (dados persistentes depois de reiniciar), `/ktholo`,
  `/ktfish` + `!use` (dispara os quatro estados de pesca do Kunftig com a boia real).

Tudo que esses scripts imprimem vai para o console do servidor (linhas começando com `ALIAS`, `SPAWN`, `EVENT`, `KT`...).
