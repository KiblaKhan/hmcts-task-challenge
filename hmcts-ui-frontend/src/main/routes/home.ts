import express, { Application, Request, Response } from 'express';
import axios from 'axios';

/**
 * Home route that optionally fetches example data from CASE_API_URL.
 * - No network calls at module load.
 * - Short timeout and safe fallback when the API isn't running.
 */
export default function registerHome(app: Application) {
  const router = express.Router();

  router.get('/', async (_req: Request, res: Response) => {
    const exampleUrl = process.env.CASE_API_URL; // e.g. http://localhost:4000/get-example-case
    let example: unknown = null;

    if (exampleUrl) {
      try {
        const { data } = await axios.get(exampleUrl, { timeout: 2000 });
        example = data;
      } catch (e: any) {
        // Log briefly; keep the page rendering
        console.warn('[home] Example API unavailable:', e?.code || e?.message || e);
      }
    }

    res.render('home/home.njk', { example });
  });

  router.get('/home', (_req, res) => res.redirect('/')); // <— add this line

  app.use('/', router);
}
