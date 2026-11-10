// Tiny static server for the demo console. Serves index.html on http://localhost:5500 and proxies two
// read-only helpers so the browser does not need CORS for them:
//   /eureka/*  ->  http://localhost:8761/eureka/*   (service registry)
//   /mail/*    ->  http://localhost:8025/*          (Mailpit API, to read the password-reset email)
// The reset link in the email points at /pages/auth/reset-password.html?token=..., which also serves index.html.
const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = 5500;
const page = () => fs.readFileSync(path.join(__dirname, "index.html"));

function proxy(req, res, port, target) {
  const up = http.request(
    { host: "127.0.0.1", port, path: target, method: req.method, headers: { accept: req.headers.accept || "*/*" } },
    (r) => { res.writeHead(r.statusCode, { "content-type": r.headers["content-type"] || "application/json" }); r.pipe(res); }
  );
  up.on("error", () => { res.writeHead(502, { "content-type": "text/plain" }); res.end("upstream not reachable"); });
  up.end();
}

http.createServer((req, res) => {
  const url = req.url;
  if (url.startsWith("/eureka/")) return proxy(req, res, 8761, url);
  if (url.startsWith("/mail/")) return proxy(req, res, 8025, url.slice(5));
  res.writeHead(200, { "content-type": "text/html; charset=utf-8", "cache-control": "no-store" });
  res.end(page());
}).listen(PORT, "127.0.0.1", () => console.log("Demo console on http://localhost:" + PORT));
