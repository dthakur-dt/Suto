# 🚨 SECURITY - Action Required Immediately

## Aapne 2 tokens leak kiye hain - Revoke karna ZARURI hai!

### 1. Telegram Bot Token: 8670... (Leak)
**Fix:**
- @BotFather -> /mybots -> T1311bot -> API Token -> Revoke
- Naya token banao

### 2. GitHub PAT: ghp_93... (Leak)
**Fix:**
- GitHub.com -> Settings (top right) -> Developer settings -> Personal access tokens -> Tokens (classic)
- Is token ko DELETE karo: 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
- Naya token banao agar chahiye, lekin kabhi share mat karo!

### Secure tarika:
- Token ko kabhi chat me, GitHub code me, ya public me mat dalo
- local.properties me rakho (ye .gitignore me hai)
- GitHub Actions me Secrets use karo
