import * as path from 'path';

import { HTTPError } from './HttpError';
import { Nunjucks } from './modules/nunjucks';

import * as bodyParser from 'body-parser';
import cookieParser from 'cookie-parser';
import express from 'express';
import { glob } from 'glob';
import favicon from 'serve-favicon';
import { globSync } from 'glob';

const { setupDev } = require('./development');

const env = process.env.NODE_ENV || 'development';
const developmentMode = env === 'development';

// Load and mount routes once
const routesDir = path.resolve(__dirname, 'routes');
const routeFiles = globSync('**/*.+(ts|js)', { cwd: routesDir, absolute: true });

export const app = express();
app.locals.ENV = env;

new Nunjucks(developmentMode).enableFor(app);

app.use(favicon(path.join(__dirname, 'public', 'assets', 'images', 'favicon.ico')));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));
app.use((req, res, next) => {
  res.setHeader('Cache-Control', 'no-cache, max-age=0, must-revalidate, no-store');
  next();
});

routeFiles
  .map(filename => require(filename))
  .forEach(mod => { if (mod && typeof mod.default === 'function') mod.default(app); });
console.log('Loaded route files:', routeFiles);

setupDev(app, developmentMode);

// Single error handler at the end
app.use((err: HTTPError, req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.error('[express error]', err?.message, err?.stack);
  res.locals.message = err?.message || 'Internal Server Error';
  res.locals.error = env === 'development' ? err : {};
  res.status(err?.status || 500);
  res.render('error');
});
