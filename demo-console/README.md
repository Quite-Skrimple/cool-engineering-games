# Demo console

A small web page for showing the backend working in a browser. It is a stand-in for demos, separate from the project's `frontend/` folder, which it does not touch.

## Run it

Start the stack first (see the main README), then, from the repository root:

```bash
node demo-console/server.js
```

Open <http://localhost:5500>. Node 18 or newer, no dependencies.

## What it does

- Pages: Sign in, Register, Forgot and Reset password, Home, Games, Dashboard (library), Admin (users), Platform (Eureka and a live probe).
- Every call goes to the gateway on `http://localhost:8000`, with the bearer token the gateway returned at login. The log at the bottom of the page lists each request and its status.
- `server.js` serves the page and passes two read-only helpers through on the same origin: `/eureka/*` to Eureka (`:8761`) and `/mail/*` to Mailpit (`:8025`), so the page can show service health and read the password-reset email.
- The reset link in that email points at `${FRONTEND_URL}/pages/auth/reset-password.html?token=...`. With the default `FRONTEND_URL` of `http://localhost:5500` the link opens this page on the reset form.

## Demo notes

- Administrator: user `admin`, password in `docker/.env` (`BOOTSTRAP_ADMIN_PASSWORD`).
- The register form is prefilled with a test player (`demo_player`) so a live demo needs no typing.
- The catalog needs a login, so the Home page shows "Sign in to load the catalog" until you sign in.
- Demo code only: the token is kept in `sessionStorage` and nothing here is hardened for production.
