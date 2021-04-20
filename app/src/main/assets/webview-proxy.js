(function() {
  const KEY = "INTERCEPT_KEY";

  xhook.before(function (request) {
    if (request.method.toUpperCase() !== 'POST') {
      return;
    }

    const url = new URL(request.url, window.location.href);

    let search = `?url=${escape(url.href)}`;
    search += `&headers=${escape(JSON.stringify(request.headers))}`;
    search += `&body=${escape(request.body)}`;

    request.method = 'GET';
    request.body = null;

    url.pathname = `/${KEY}`;
    url.search = search;

    request.url = url.href;
  });

  console.log('webview-proxy.js | Loaded successfully');
})();
