# Deployment Guide - Roberta Furucho Platform

This guide covers deploying the Furucho platform using tools from the **GitHub Student Developer Pack**.

## 📦 Architecture Overview

```text
┌─────────────────────────────────────────────────────────┐
│                    PRODUCTION                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   ┌─────────────┐       ┌─────────────┐                │
│   │  Frontend   │       │   Backend   │                │
│   │ (React SSR) │◄─────►│ (Spring Boot)│               │
│   │ DigitalOcean│       │    Azure    │                │
│   │ App Platform│       │ App Service │                │
│   └─────────────┘       └──────┬──────┘                │
│                                │                        │
│                         ┌──────▼──────┐                │
│                         │  PostgreSQL │                │
│                         │    Azure    │                │
│                         └─────────────┘                │
│                                                         │
│   ┌─────────────┐                                      │
│   │   Domain    │                                      │
│   │  Namecheap  │                                      │
│   │   or Name   │                                      │
│   └─────────────┘                                      │
└─────────────────────────────────────────────────────────┘
```

## 🎓 GitHub Student Pack Resources Used

| Tool                 | Value           | Purpose                      |
| -------------------- | --------------- | ---------------------------- |
| Azure for Students   | $100 credit     | Backend hosting, PostgreSQL  |
| DigitalOcean         | $200 credit     | Frontend hosting             |
| Namecheap            | Free .me domain | Primary domain               |
| Name.com             | Free domain     | Alternative domain           |
| MongoDB Atlas        | $50 credit      | Optional NoSQL storage       |

---

## 🔧 Backend Deployment (Azure App Service)

### Prerequisites

- Azure for Students account ($100 credit)
- Azure CLI installed (`az`)
- GitHub repository with secrets configured

### Step 1: Create Azure Resources

```bash
# Login to Azure
az login

# Create resource group
az group create \
  --name furucho-rg \
  --location brazilsouth

# Create App Service Plan (Free tier F1)
az appservice plan create \
  --name furucho-plan \
  --resource-group furucho-rg \
  --sku F1 \
  --is-linux

# Create Web App
az webapp create \
  --name furucho-api \
  --resource-group furucho-rg \
  --plan furucho-plan \
  --runtime "JAVA:17-java17"

# Create PostgreSQL Server (Basic tier)
az postgres flexible-server create \
  --name furucho-db \
  --resource-group furucho-rg \
  --location brazilsouth \
  --admin-user furuchoadmin \
  --admin-password "YOUR_SECURE_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32
```

### Step 2: Configure Environment Variables

```bash
az webapp config appsettings set \
  --name furucho-api \
  --resource-group furucho-rg \
  --settings \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL="jdbc:postgresql://furucho-db.postgres.database.azure.com:5432/furuchodb" \
    DATABASE_USERNAME="furuchoadmin" \
    DATABASE_PASSWORD="YOUR_SECURE_PASSWORD"
```

### Step 3: Set Up GitHub Secrets

In your GitHub repository, go to **Settings → Secrets and variables → Actions** and add:

| Secret              | Value                                  |
| ------------------- | -------------------------------------- |
| `AZURE_CREDENTIALS` | Output from `az ad sp create-for-rbac` |
| `AZURE_WEBAPP_NAME` | `furucho-api`                          |

Generate Azure credentials:

```bash
az ad sp create-for-rbac \
  --name "furucho-github-actions" \
  --role contributor \
  --scopes /subscriptions/{subscription-id}/resourceGroups/furucho-rg \
  --sdk-auth
```

---

## 🌐 Frontend Deployment (DigitalOcean)

### DigitalOcean Prerequisites

- DigitalOcean account ($200 credit from Student Pack)
- DigitalOcean CLI (`doctl`) installed

### Option A: App Platform (Recommended)

1. Go to [DigitalOcean App Platform](https://cloud.digitalocean.com/apps)
2. Click **Create App**
3. Connect your GitHub repository
4. Configure:
   - **Source**: GitHub
   - **Branch**: `main`
   - **Type**: Web Service
   - **Build Command**: `npm run build`
   - **Run Command**: `npm run start`
   - **HTTP Port**: `3000`

5. Add environment variables:

   ```text
   NODE_ENV=production
   API_URL=https://furucho-api.azurewebsites.net
   ```

### Option B: Droplet with Docker

```bash
# Create Droplet
doctl compute droplet create furucho-frontend \
  --image docker-20-04 \
  --size s-1vcpu-1gb \
  --region nyc1

# SSH into Droplet
doctl compute ssh furucho-frontend

# On the Droplet:
git clone https://github.com/YOUR_USERNAME/furucho.git
cd furucho
docker build -t furucho-frontend .
docker run -d -p 80:3000 furucho-frontend
```

### GitHub Secrets for DigitalOcean

| Secret                      | Value                            |
| --------------------------- | -------------------------------- |
| `DIGITALOCEAN_ACCESS_TOKEN` | Your API token from DO dashboard |
| `DIGITALOCEAN_APP_ID`       | App ID from App Platform         |

---

## 🌍 Domain Configuration (Namecheap)

### Step 1: Claim Free Domain

1. Visit [Namecheap GitHub Student Pack](https://nc.me/)
2. Sign in with GitHub
3. Claim your free `.me` domain (e.g., `robertafurucho.me`)

### Step 2: Configure DNS

Add these DNS records in Namecheap dashboard:

| Type  | Host | Value                         | TTL  |
| ----- | ---- | ----------------------------- | ---- |
| A     | @    | DigitalOcean Droplet IP       | Auto |
| A     | www  | DigitalOcean Droplet IP       | Auto |
| CNAME | api  | furucho-api.azurewebsites.net | Auto |

### Step 3: SSL Certificates

**For DigitalOcean App Platform**: SSL is automatic.

**For Droplet**: Use Let's Encrypt:

```bash
sudo apt install certbot
sudo certbot --nginx -d robertafurucho.me -d www.robertafurucho.me
```

---

## 🔒 Security Checklist

- [ ] Enable HTTPS on all endpoints
- [ ] Configure CORS for production domains only
- [ ] Set secure database passwords
- [ ] Enable rate limiting (already in code)
- [ ] Configure WAF (Azure has built-in)
- [ ] Set up monitoring (Azure Application Insights)

---

## 💰 Cost Estimation (Monthly)

| Service                          | Cost                              |
| -------------------------------- | --------------------------------- |
| Azure App Service (F1)           | **Free**                          |
| Azure PostgreSQL (B1ms)          | ~$15/mo (covered by $100 credit)  |
| DigitalOcean App Platform (Basic)| ~$5/mo (covered by $200 credit)   |
| Domain (.me)                     | **Free** first year               |
| **Total Year 1**                 | **~$0** (within credits)          |

---

## 🚀 CI/CD Pipeline

The GitHub Actions workflows handle:

1. **On Pull Request**:
   - Run tests (frontend + backend)
   - Type checking
   - Build verification

2. **On Push to Main**:
   - All PR checks +
   - Deploy backend to Azure
   - Deploy frontend to DigitalOcean

---

## 📞 Support

- **Azure**: [Azure for Students Portal](https://azure.microsoft.com/free/students/)
- **DigitalOcean**: [DigitalOcean Community](https://www.digitalocean.com/community)
- **Namecheap**: [Namecheap Support](https://www.namecheap.com/support/)
