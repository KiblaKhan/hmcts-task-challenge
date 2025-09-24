import path from 'path';
import nunjucks from 'nunjucks';
import express from 'express';

export class Nunjucks {
  constructor(private isDev: boolean) { }

  enableFor(app: express.Express) {
    const appViews = path.join(__dirname, '..', '..', 'views');
    const govukViews = path.dirname(
      require.resolve('govuk-frontend/govuk/template.njk')
    );

    const isDev = process.env.NODE_ENV === 'development';
    const isTest = process.env.NODE_ENV === 'test';

    const env = nunjucks.configure([appViews, govukViews], {
      autoescape: true,
      express: app,
      noCache: isDev || isTest,
      watch: isDev && !isTest,
    });

    app.set('view engine', 'njk');
    app.set('views', appViews);

    // GOV.UK base template needs this
    app.locals.asset_path = '/assets';
    env.addGlobal('asset_path', '/assets');

    // Handy filters
    env.addFilter('fallback', (v: any, alt: any) =>
      v === undefined || v === null || v === '' ? alt : v
    );

    env.addFilter('fmtDate', (iso?: string) => {
      if (!iso) return '-';
      try {
        const d = new Date(iso);
        return d.toLocaleString('en-GB', {
          day: '2-digit',
          month: 'short',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        });
      } catch {
        return iso;
      }
    });
  }
}
