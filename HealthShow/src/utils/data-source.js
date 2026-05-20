import Cookies from 'js-cookie'

export const DATA_SOURCE_COOKIE = 'Health-Data-Source'
export const DATA_SOURCE_HEADER = 'X-Health-Data-Source'
export const DATA_SOURCE_NEW = 'new'
export const DATA_SOURCE_OLD = 'old'
const DATA_SOURCE_INITIALIZED_COOKIE = `${DATA_SOURCE_COOKIE}-Initialized`
const COOKIE_OPTIONS = { expires: 365, sameSite: 'Lax' }

export const DEFAULT_DATA_SOURCE = import.meta.env.VITE_DEFAULT_DATA_SOURCE === DATA_SOURCE_OLD
  ? DATA_SOURCE_OLD
  : DATA_SOURCE_NEW

function normalizeDataSource(source) {
  return source === DATA_SOURCE_OLD || source === DATA_SOURCE_NEW ? source : ''
}

function markDataSourceInitialized() {
  Cookies.set(DATA_SOURCE_INITIALIZED_COOKIE, '1', COOKIE_OPTIONS)
}

function shouldResetStaleDevCookie(source) {
  return import.meta.env.DEV &&
    DEFAULT_DATA_SOURCE === DATA_SOURCE_OLD &&
    source === DATA_SOURCE_NEW &&
    !Cookies.get(DATA_SOURCE_INITIALIZED_COOKIE)
}

export function getDataSource() {
  const source = normalizeDataSource(Cookies.get(DATA_SOURCE_COOKIE))
  if (source && !shouldResetStaleDevCookie(source)) {
    markDataSourceInitialized()
    return source
  }
  setDataSource(DEFAULT_DATA_SOURCE)
  return DEFAULT_DATA_SOURCE
}

export function setDataSource(source) {
  const normalized = source === DATA_SOURCE_OLD ? DATA_SOURCE_OLD : DATA_SOURCE_NEW
  Cookies.set(DATA_SOURCE_COOKIE, normalized, COOKIE_OPTIONS)
  markDataSourceInitialized()
  return normalized
}
