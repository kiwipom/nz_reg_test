# 

**Unauthenticated**
- Description: Public users accessing company information, without logging in
- Permissions: `read:companies`

**Public Role**
- Description: An end user with a login (e.g. A Company Director)
- Permissions: `read:companies` - every registry user has this permission
- In addition, Users with this role will need `write` access to only *their* Entity (change directors, file annual return, etc)

**Registrar Role**
- Description: Companies Office registrars
- Permissions: `read:companies`, `write:companies`, `read:directors`, `write:directors`, `read:shareholders`, `write:shareholders`

**Internal Ops Role**
- Description: Internal operations staff
- Permissions: `read:companies`, `read:directors`, `read:shareholders`

**Admin Role**
- Description: System administrators
- Permissions: `admin:all`
