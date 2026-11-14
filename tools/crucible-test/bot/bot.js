// Test bot for the Crucible 1.7.10 server: connects with the FML handshake, logs chat and plugin channel messages,
// and sends every new line of bot.in as a chat message/command. Usage: node bot.js [username]
const mc = require('minecraft-protocol')
const forge = require('minecraft-protocol-forge')
const fs = require('fs')

const username = process.argv[2] || 'Tester'
const inFile = __dirname + '/bot.in'
fs.writeFileSync(inFile, '')

function log (...a) { console.log(new Date().toISOString().substr(11, 8), ...a) }

function chatText (json) {
  try {
    const o = typeof json === 'string' ? JSON.parse(json) : json
    const walk = (c) => {
      if (typeof c === 'string') return c
      let s = (c.text || '') + (c.translate ? '[' + c.translate + (c.with ? ':' + c.with.map(walk).join(',') : '') + ']' : '')
      if (c.clickEvent) s += '{click ' + c.clickEvent.action + '=' + c.clickEvent.value + '}'
      if (c.hoverEvent) s += '{hover ' + c.hoverEvent.action + '=' + JSON.stringify(c.hoverEvent.value) + '}'
      if (c.extra) s += c.extra.map(walk).join('')
      return s
    }
    return walk(o)
  } catch (e) { return String(json) }
}

mc.ping({ host: 'localhost', port: 25565, version: '1.7.10' }, (err, res) => {
  if (err) { log('PING ERROR', err.message); process.exit(1) }
  const mods = (res.modinfo && res.modinfo.modList) || []
  log('PING ok, server mods:', mods.map(m => m.modid + '@' + m.version).join(' '))
  // 1.7.10 prefixes plugin message data with a signed short, but FML sends registry data over 32767 bytes with an
  // extended length. Packets are framed, so read the rest of the frame instead.
  const customPackets = { '1.7': { play: { toClient: { types: {
    packet_custom_payload: ['container', [{ name: 'channel', type: 'string' }, { name: 'skipLength', type: 'i16' }, { name: 'data', type: 'restBuffer' }]]
  } } } } }
  const client = mc.createClient({ host: 'localhost', port: 25565, username, version: '1.7.10', auth: 'offline', customPackets })
  // FML's varshort: when the high bit of the length is set, one more length byte precedes the data
  const emit = client.emit.bind(client)
  client.emit = (event, params, ...rest) => {
    if (event === 'custom_payload' && params && params.skipLength < 0 && params.data) params.data = params.data.slice(1)
    return emit(event, params, ...rest)
  }
  client.fml17 = true
  forge.forgeHandshake(client, { forgeMods: mods })

  client.on('login', () => log('LOGIN entityId', client.entityId))
  client.on('chat', (p) => log('CHAT', chatText(p.message)))
  client.on('custom_payload', (p) => {
    const data = p.data ? Buffer.from(p.data) : Buffer.alloc(0)
    log('PAYLOAD', p.channel, data.length + 'b', JSON.stringify(data.toString('latin1').replace(/[^\x20-\x7e]/g, '.').substr(0, 160)))
  })
  client.on('kick_disconnect', (p) => log('KICK', chatText(p.reason)))
  client.on('end', (r) => { log('END', r); process.exit(0) })
  client.on('error', (e) => log('ERROR', e.message))
  client.on('position', (p) => {
    log('POSITION', p.x.toFixed(1), p.y.toFixed(1), p.z.toFixed(1))
    // 1.7.10: the client must confirm the position
    client.write('position_look', { x: p.x, stance: p.y + 1.62, y: p.y, z: p.z, yaw: p.yaw, pitch: p.pitch, onGround: true })
  })
  client.on('update_health', (p) => log('HEALTH', p.health, 'food', p.food))
  let lastLiving = null
  client.on('spawn_entity_living', (p) => { lastLiving = p.entityId; log('SPAWN_LIVING id', p.entityId, 'type', p.type) })
  client.on('scoreboard_objective', (p) => log('SCOREBOARD', JSON.stringify(p)))
  client.on('player_info', (p) => log('PLAYER_INFO', JSON.stringify(p).substr(0, 120)))
  client.on('world_particles', (p) => log('PARTICLE', p.particleName, p.particles))
  client.on('named_sound_effect', (p) => log('SOUND', p.soundName))
  client.on('set_slot', (p) => { if (p.item && p.item.blockId > 0) log('SLOT', p.windowId, p.slot, JSON.stringify(p.item).substr(0, 160)) })
  client.on('open_window', (p) => log('OPEN_WINDOW', JSON.stringify(p)))

  // keep the bot "on the ground" so the server doesn't kick it
  setInterval(() => { if (client.state === 'play') client.write('flying', { onGround: true }) }, 1000)

  let sent = 0
  setInterval(() => {
    const lines = fs.readFileSync(inFile, 'utf8').split('\n').filter(l => l.length)
    for (; sent < lines.length; sent++) {
      log('SEND', lines[sent])
      if (lines[sent] === '!use') {
        // right click with the held item in the air (casts/reels in a fishing rod)
        client.write('block_place', { location: { x: -1, y: 255, z: -1 }, direction: -1,
          heldItem: { blockId: 346, itemCount: 1, itemDamage: 0 }, cursorX: 0, cursorY: 0, cursorZ: 0 })
        continue
      }
      if (lines[sent].startsWith('!look ')) {
        const [yaw, pitch] = lines[sent].substr(6).split(' ').map(Number)
        client.write('look', { yaw, pitch, onGround: true })
        continue
      }
      if (lines[sent] === '!attack') {
        if (lastLiving !== null) client.write('use_entity', { target: lastLiving, mouse: 1 })
        continue
      }
      client.write('chat', { message: lines[sent] })
    }
  }, 300)
})
