import { OpenAPIHono } from '@hono/zod-openapi'
import { youtubeService } from './services'
import { TAMIL_CATEGORIES } from '../../../shared/constants/categories'
import type { Routes } from '#common/types'

export class YouTubeController implements Routes {
  public controller: OpenAPIHono

  constructor() {
    this.controller = new OpenAPIHono()
  }

  public initRoutes() {
    // 1. /api/youtube/search (and also /api/tamil/search)
    this.controller.get('/youtube/search', async (c) => {
      const q = c.req.query('q') || ''
      const maxResults = parseInt(c.req.query('maxResults') || '20', 10)

      if (!q.trim()) {
        return c.json({ success: true, data: { results: [] } })
      }

      try {
        const results = await youtubeService.searchTamilSongs(q, maxResults)
        return c.json({ success: true, data: { results, total: results.length } })
      } catch (error: any) {
        return c.json(
          { success: false, message: error?.message || 'Failed to search Tamil songs' },
          500
        )
      }
    })

    // 2. /api/youtube/trending
    this.controller.get('/youtube/trending', async (c) => {
      const maxResults = parseInt(c.req.query('maxResults') || '20', 10)
      try {
        const results = await youtubeService.getTrending(maxResults)
        return c.json({ success: true, data: { results, total: results.length } })
      } catch (error: any) {
        return c.json(
          { success: false, message: error?.message || 'Failed to fetch trending songs' },
          500
        )
      }
    })

    // 3. /api/youtube/categories
    this.controller.get('/youtube/categories', (c) => {
      return c.json({ success: true, data: TAMIL_CATEGORIES })
    })
  }
}
