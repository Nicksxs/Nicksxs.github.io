(function() {
  const configElement = document.querySelector('script[data-cloudflare-page-counter-config]');
  const config = configElement ? JSON.parse(configElement.textContent || '{}') : {};
  const endpoint = (config.endpoint || '').replace(/\/$/, '');
  const cacheEnabled = config.local_cache !== false;
  const cachePrefix = `cloudflare-page-counter:${config.site || location.hostname}:`;

  if (!endpoint) return;

  const normalizePath = path => {
    try {
      const url = new URL(path, location.origin);
      return decodeURI(url.pathname.replace(/\/index\.html$/, '/'));
    } catch (error) {
      return decodeURI(path || location.pathname);
    }
  };

  const request = payload => {
    const controller = new AbortController();
    const timeout = Number(config.timeout) || 5000;
    const timer = setTimeout(() => controller.abort(), timeout);

    return fetch(endpoint, {
      method : 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body       : JSON.stringify(payload),
      credentials: 'omit',
      signal     : controller.signal
    }).then(response => {
      clearTimeout(timer);
      if (!response.ok) throw new Error(`Counter request failed: ${response.status}`);
      return response.json();
    });
  };

  const render = counts => {
    document.querySelectorAll('.cloudflare-page-counter').forEach(element => {
      const path = normalizePath(element.dataset.path);
      const count = counts[path];
      const target = element.querySelector('.cloudflare-page-counter-count');
      if (target && typeof count === 'number') target.innerText = count;
    });
  };

  const loadCounters = () => {
    const elements = [...document.querySelectorAll('.cloudflare-page-counter')];
    if (!elements.length) return;

    const entries = elements.map(element => ({
      path : normalizePath(element.dataset.path),
      title: element.dataset.title || document.title
    }));

    const hitIndex = elements.findIndex(element => element.dataset.increment === 'true');
    let action = hitIndex === -1 ? 'get' : 'hit';
    let selectedEntries = entries;

    if (action === 'hit') {
      const key = cachePrefix + entries[hitIndex].path;
      selectedEntries = [entries[hitIndex]];

      if (cacheEnabled && localStorage.getItem(key)) {
        action = 'get';
        selectedEntries = entries;
      } else if (cacheEnabled) {
        localStorage.setItem(key, String(Date.now()));
      }
    }

    request({
      action,
      site   : config.site || location.hostname,
      entries: selectedEntries
    }).then(data => {
      render(data.counts || {});
    }).catch(error => {
      console.error('Cloudflare Page Counter Error', error);
    });
  };

  document.addEventListener('DOMContentLoaded', loadCounters);
  document.addEventListener('page:loaded', loadCounters);
})();
