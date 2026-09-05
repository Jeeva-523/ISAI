import { serve } from '@hono/node-server'
import { AlbumController, ArtistController, SearchController, SongController } from '#modules/index'
import { PlaylistController } from '#modules/playlists/controllers'
import { YouTubeController } from './modules/youtube/controllers'
import { App } from './app'

const app = new App([
  new SearchController(),
  new SongController(),
  new AlbumController(),
  new ArtistController(),
  new PlaylistController(),
  new YouTubeController()
]).getApp()

const port = Number(process.env.PORT) || 3000

// In Node.js environment, start HTTP server using @hono/node-server
if (typeof (globalThis as any).Bun === 'undefined') {
  serve({
    fetch: app.fetch,
    port
  }, (info) => {
    console.log(`[ISAI Backend] Server listening on http://localhost:${info.port}`)
  })
}

export default app
