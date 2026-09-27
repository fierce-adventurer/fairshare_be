# fairshare_be
This the backend of the split wise application

## Docker CI/CD on Push

This repository now includes `/home/runner/work/fairshare_be/fairshare_be/.github/workflows/docker-deploy.yml`.

### What it does
- Triggers on push to `main` and manual workflow dispatch.
- Builds Docker image from `/home/runner/work/fairshare_be/fairshare_be/Dockerfile`.
- Pushes image to GHCR with tags:
  - `sha-<full_commit_sha>`
  - `latest`
- Deploys to your server over SSH after a successful push.

### Required GitHub Secrets

Set these in **Repository Settings → Secrets and variables → Actions**:
- `DEPLOY_HOST`: server IP or hostname.
- `DEPLOY_USER`: SSH user on the server.
- `DEPLOY_SSH_KEY`: private SSH key for deployment.
- `DEPLOY_PORT`: SSH port (optional; defaults to `22`).
- `GHCR_USERNAME`: GHCR username used by the server to pull images.
- `GHCR_PULL_TOKEN`: GHCR token with package read permission.

### Runtime env configuration (outside image)

On your server, create and maintain:
- `/opt/fairshare/.env`

Use it to keep runtime config (DB URL, JWT secret, OAuth keys, etc.) outside the Docker image.
The deployment workflow starts containers with:
- `--env-file /opt/fairshare/.env`

### Safe rollout behavior

Deployment uses a candidate container first:
1. Pulls new image.
2. Starts `fairshare-api-candidate` on port `18080`.
3. Validates `http://localhost:18080/actuator/health`.
4. Only if healthy, replaces `fairshare-api` on port `8080`.
5. Keeps restart policy `unless-stopped`.

If candidate healthcheck fails, live container is left untouched.

### Environment protection (recommended)

Create a GitHub Environment named `production` and add protection rules (required reviewers) to enforce approval before deploy.

### End-to-end verification

1. Push a small commit to `main`.
2. Open **Actions** tab and confirm workflow success.
3. Confirm a new image exists in GHCR with `sha-<commit_sha>`.
4. Verify server container is running new image:
   - `docker ps`
   - `docker inspect fairshare-api --format='{{.Config.Image}}'`
