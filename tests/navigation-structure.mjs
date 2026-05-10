import fs from 'node:fs';
import appRoutes from '../src/router/app-routes.mjs';
import { NAV_GROUPS, buildMobileNavItems, resolveMenuPath } from '../src/layout/menu/navigation.mjs';

function readText(relativePath) {
  return fs.readFileSync(new URL(relativePath, import.meta.url), 'utf8');
}

function flattenRoutes(routes, basePath = '') {
  const items = [];

  for (const route of routes || []) {
    const fullPath = resolveMenuPath(basePath, route.path || '');
    items.push({
      ...route,
      fullPath
    });

    if (route.children?.length) {
      items.push(...flattenRoutes(route.children, fullPath));
    }
  }

  return items;
}

function buildLeafRoutes(routes, basePath = '') {
  const leaves = [];

  for (const route of routes || []) {
    const fullPath = resolveMenuPath(basePath, route.path || '');

    if (route.children?.length) {
      leaves.push(...buildLeafRoutes(route.children, fullPath));
      continue;
    }

    leaves.push({
      ...route,
      fullPath
    });
  }

  return leaves;
}

const flatRoutes = flattenRoutes(appRoutes);
const leafRoutes = buildLeafRoutes(appRoutes);
const navGroupMap = new Map(NAV_GROUPS.map((group) => [group.key, group]));
const mobileNavItems = buildMobileNavItems(appRoutes);

const issues = [];

const legacyRouteFiles = [
  '../src/router/app-routes.js',
  '../src/router/health-monitor.js',
  '../src/router/alert-management.js'
];

for (const legacyPath of legacyRouteFiles) {
  if (fs.existsSync(new URL(legacyPath, import.meta.url))) {
    issues.push(`legacy route file still exists: ${legacyPath.replace('../src/', 'src/')}`);
  }
}

const routerIndexSource = readText('../src/router/index.js');
if (!routerIndexSource.includes("import appRoutes from './app-routes.mjs'")) {
  issues.push('router/index.js is not sourcing app routes from ./app-routes.mjs');
}

const routeAccessSource = readText('../src/router/app-route-access.js');
if (!routeAccessSource.includes("import appRoutes from './app-routes.mjs'")) {
  issues.push('router/app-route-access.js is not sourcing app routes from ./app-routes.mjs');
}

const appRoutesSource = readText('../src/router/app-routes.mjs');
if (!appRoutesSource.includes("import healthMonitorRouter from './health-monitor.mjs'")) {
  issues.push('router/app-routes.mjs is not sourcing health monitor routes from ./health-monitor.mjs');
}
if (!appRoutesSource.includes("import alertManagementRouter from './alert-management.mjs'")) {
  issues.push('router/app-routes.mjs is not sourcing alert management routes from ./alert-management.mjs');
}

const visibleNavLeaves = leafRoutes.filter((route) => !route.hidden && !route.redirect);

for (const route of visibleNavLeaves) {
  if (!route.meta?.navGroup) {
    issues.push(`missing navGroup: ${route.fullPath}`);
  }

  if (typeof route.meta?.navOrder !== 'number') {
    issues.push(`missing navOrder: ${route.fullPath}`);
  }

  if (route.meta?.navGroup && !navGroupMap.has(route.meta.navGroup)) {
    issues.push(`unknown navGroup "${route.meta.navGroup}": ${route.fullPath}`);
  }
}

for (const group of NAV_GROUPS) {
  const groupRoutes = visibleNavLeaves.filter((route) => route.meta?.navGroup === group.key);
  const orderMap = new Map();

  for (const route of groupRoutes) {
    const key = route.meta.navOrder;
    if (!orderMap.has(key)) {
      orderMap.set(key, []);
    }
    orderMap.get(key).push(route.fullPath);
  }

  for (const [order, paths] of orderMap.entries()) {
    if (paths.length > 1) {
      issues.push(`duplicate navOrder in group "${group.key}" (${order}): ${paths.join(', ')}`);
    }
  }
}

const knownPaths = new Set(flatRoutes.map((route) => route.fullPath));
for (const item of mobileNavItems) {
  if (!knownPaths.has(item.path)) {
    issues.push(`mobile nav path missing: ${item.path}`);
  }

  for (const match of item.matches || []) {
    const exactHit = knownPaths.has(match);
    const prefixHit = [...knownPaths].some((path) => path.startsWith(match));
    if (!exactHit && !prefixHit) {
      issues.push(`mobile nav match has no route target: ${match}`);
    }
  }
}

if (issues.length > 0) {
  console.error('Navigation structure check failed.');
  for (const issue of issues) {
    console.error(`- ${issue}`);
  }
  process.exit(1);
}

const summary = {
  navGroups: NAV_GROUPS.length,
  visibleNavLeaves: visibleNavLeaves.length,
  mobileNavItems: mobileNavItems.length
};

console.log(JSON.stringify(summary, null, 2));
