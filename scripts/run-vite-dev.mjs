import childProcess from 'node:child_process'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { pathToFileURL } from 'node:url'

const originalSpawn = childProcess.spawn.bind(childProcess)
const originalSpawnSync = childProcess.spawnSync.bind(childProcess)
function shouldWrapEsbuild(command, args) {
  const isArray = Array.isArray(args)
  const hasService = isArray && args.some((arg) => typeof arg === 'string' && arg.startsWith('--service='))
  if (process.env.DEBUG_ESBUILD_WRAP) {
    console.error('[shouldWrap]', { isArray, hasService, command })
  }
  return isArray && hasService
}

function wrapSpawn(command, args = [], options = {}) {
  if (process.env.DEBUG_ESBUILD_WRAP) {
    console.error('[spawn]', command, Array.isArray(args) ? args : String(args))
  }
  if (!shouldWrapEsbuild(command, args)) {
    return originalSpawn(command, args, options)
  }

  const commandLine = `& '${command}' ${args.map((arg) => String(arg)).join(' ')}`
  return originalSpawn('powershell.exe', ['-NoLogo', '-NoProfile', '-Command', commandLine], options)
}

function wrapSpawnSync(command, args = [], options = {}) {
  if (process.env.DEBUG_ESBUILD_WRAP) {
    console.error('[spawnSync]', command, Array.isArray(args) ? args : String(args))
  }
  if (!shouldWrapEsbuild(command, args)) {
    return originalSpawnSync(command, args, options)
  }

  const commandLine = `& '${command}' ${args.map((arg) => String(arg)).join(' ')}`
  return originalSpawnSync('powershell.exe', ['-NoLogo', '-NoProfile', '-Command', commandLine], options)
}

childProcess.spawn = wrapSpawn
childProcess.spawnSync = wrapSpawnSync

const require = createRequire(import.meta.url)
const esbuild = require('esbuild')
const originalBuildSync = esbuild.buildSync.bind(esbuild)
const originalTransformSync = esbuild.transformSync.bind(esbuild)

require.cache[require.resolve('esbuild')].exports = {
  ...esbuild,
  build: async (options) => originalBuildSync(options),
  transform: async (input, options) => originalTransformSync(input, options)
}

const viteCli = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'node_modules', 'vite', 'bin', 'vite.js')
await import(pathToFileURL(viteCli).href)
