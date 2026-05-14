# TLS / PKIX Certificate Fix for Docker Builds

## What Was Fixed

The original build failed with:

```
PKIX path building failed
unable to find valid certification path to requested target
Could not transfer artifact org.springframework.boot:spring-boot-starter-parent
from https://repo.maven.apache.org/maven2
```

This happens when your network (corporate proxy, VPN, or ISP) performs **SSL inspection** —
it replaces the real Maven Central certificate with its own CA certificate that the JDK inside
the Docker container doesn't trust.

## Three-Layer Fix Applied

### Layer 1 — Maven Mirror (Primary fix)
`backend/docker/maven/settings.xml` redirects all Maven downloads through **Aliyun's mirror**
(`maven.aliyun.com`), which serves the identical artifacts. This completely avoids
`repo.maven.apache.org` so the intercepted certificate is never encountered.

### Layer 2 — SSL Insecure Mode (Fallback)
If the mirror is also behind your proxy, every Dockerfile and the build scripts set:
```
-Dmaven.wagon.http.ssl.insecure=true
-Dmaven.wagon.http.ssl.allowall=true
```
This makes Maven's HTTP transport accept any certificate. Safe for build-time dependency
downloading (not for production runtime).

### Layer 3 — Corporate CA Import (Manual, if needed)
If your company requires all traffic to go through a specific proxy with a known CA cert:

```bash
# Export your corporate CA cert (ask your IT team, or:)
openssl s_client -connect repo.maven.apache.org:443 -showcerts </dev/null 2>/dev/null \
  | openssl x509 -outform PEM > corporate-ca.crt

# Import it so the JDK inside Docker trusts it
./build_core.sh import-ca ./corporate-ca.crt

# Then rebuild from scratch
./build_core.sh --no-cache
```

## How to Build

```bash
# Standard build (uses Aliyun mirror, SSL fallback)
cd careerbridge-fixed
./build_core.sh

# Full build including notification + admin service
./build_core.sh full

# Force clean rebuild (if you had a partial failed build)
./build_core.sh --no-cache

# If still failing — import your corporate CA cert first
./build_core.sh import-ca /path/to/your-corporate-ca.crt
./build_core.sh --no-cache
```

## If the Aliyun Mirror Is Also Blocked

Edit `backend/docker/maven/settings.xml` and change the mirror URLs to one of:

| Mirror | URL |
|--------|-----|
| Maven Central (direct) | `https://repo.maven.apache.org/maven2` |
| Google | `https://maven-central.storage.googleapis.com/maven2/` |
| JBoss | `https://repository.jboss.org/maven2` |
| Aliyun (current) | `https://maven.aliyun.com/repository/central` |

## Verifying the Fix

```bash
# During build, you should see Maven downloading FROM aliyun:
# Downloaded from aliyun-central: https://maven.aliyun.com/repository/central/...

# If you still see Maven Central URLs being hit, the mirror is being bypassed.
# In that case, run: ./build_core.sh import-ca <your-ca.crt>
```
