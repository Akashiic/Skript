// minecraft-protocol-forge expects the 1.8 handshake (several RegistryData packets with 'hasMore'); Minecraft 1.7.10
// sends a single one (ModIdData). bot.js sets client.fml17 = true and this makes the handshake accept it.
const fs = require('fs')
const file = require.resolve('minecraft-protocol-forge/src/client/forgeHandshake.js')
let src = fs.readFileSync(file, 'utf8')
if (!src.includes('client.fml17')) {
  src = src.replace('if (parsed.data.hasMore === false) {', 'if (parsed.data.hasMore === false || client.fml17) {')
  fs.writeFileSync(file, src)
  console.log('patched', file)
}
