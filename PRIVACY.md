# Privacy Policy for Tsundoku

**Last Updated: June 28, 2026**

We are the developers of **Tsundoku**, an app for tracking your manga orders —
shipping, delays, and what you're reading next. This policy explains what personal
data the app handles, why, and what choices you have.

Tsundoku uses a backend server so your orders stay in sync across your devices.
That means some of the data you enter is sent to and stored on the Tsundoku
server. This policy describes exactly what that involves.

---

### 1. Information We Collect

We only collect what is needed to run the app. We do **not** collect data for
advertising, profiling, or sale.

**Account data**

* **Email address** — used to sign you in, verify your account, and recover your
  password.
* **Username** — your display name.
* **Password** — stored on the server only in **hashed and salted** form. We never
  store or have access to your password in plain text.
* **Account status** — your email-verification status and an internal user ID.

There are no guest/anonymous accounts; an account always has an email address.

**Content you create**

* **Manga orders** — the orders you record: titles/series, volumes, prices,
  currency, order and delivery dates, status, delay reports, and reading-list
  entries, with their timestamps.

This content belongs to your account only; it is not shared with other users.

**Technical data**

* **Server logs** — standard request information (such as IP address and request
  metadata) that any web server records in order to operate the service securely.

We do **not** collect a push-notification token — the app has no push
notifications.

---

### 2. How We Use Your Information

We use the data above only to:

* Provide the core service — store and sync your manga orders and reading list.
* Authenticate you and keep your account secure.
* Send you **transactional emails** — account verification and password reset.
  These are not marketing emails, and they are the only emails we send.
* Keep the service secure, prevent abuse, and diagnose problems.

We do **not** use your data for advertising, build profiles, or sell/rent your
data to anyone.

---

### 3. Legal Bases for Processing (GDPR)

If you are in the European Economic Area, we process your data on these legal
bases:

* **Performance of a contract** — operating your account and providing the
  order-tracking service you asked for.
* **Legitimate interests** — keeping the service secure, preventing abuse, and
  maintaining reliability.

---

### 4. Third-Party Services

The app itself does not include analytics, crash reporting, advertising,
third-party sign-in, or push notifications. To operate, the service relies on a
small number of providers ("sub-processors"):

* **Mailgun (Sinch)** — email delivery provider that sends transactional emails
  (verification and password reset). The provider receives the recipient email
  address and message content needed to deliver the email. See Mailgun's privacy
  policy: [https://www.mailgun.com/privacy-policy/](https://www.mailgun.com/privacy-policy/)
* **Cloudflare** — DNS and reverse-proxy/CDN in front of the backend; processes
  connection metadata (e.g. IP address) to route and protect traffic. See:
  [https://www.cloudflare.com/privacypolicy/](https://www.cloudflare.com/privacypolicy/)
* **Hosting** — the Tsundoku backend runs on a first-party server located in the
  **European Union (Germany)**.
* **App stores** — if you installed from a store (e.g. Google Play), that store
  collects data under its own privacy policy. See:
  [https://policies.google.com/privacy](https://policies.google.com/privacy)
* **GitHub** — if you interact with the project's source or issues on GitHub,
  GitHub's own privacy practices apply.

---

### 5. Data Storage and Security

* **In transit:** all communication with the server uses encrypted HTTPS.
* **On your device:** authentication tokens are kept in the operating system's
  secure storage (Android Keystore / iOS Keychain). Your orders are also cached
  locally so the app works offline.
* **On the server:** your data is stored on servers in the EU; passwords are
  hashed and salted.
* We recommend enabling your device's lock screen (PIN, fingerprint or face
  unlock) to protect the data cached on your device.

---

### 6. Data Retention and Deletion

* Your account and the content you create are kept for as long as your account is
  active.
* **Uninstalling the app** removes the local cache from your device, but does
  **not** delete your data from the server.
* To delete your account and associated personal data from the server, see
  [`docs/ACCOUNT_DELETION.md`](docs/ACCOUNT_DELETION.md) or use the contact channel
  in Section 9. We delete your personal data within a reasonable period, except
  where we are required to retain certain records by law.

---

### 7. Your Rights (GDPR)

Depending on your location, you have the right to:

* **Access** the personal data we hold about you.
* **Rectify** inaccurate data (you can edit much of it directly in the app).
* **Erase** your data ("right to be forgotten").
* **Port** your data to another service.
* **Restrict** or **object** to certain processing.
* **Lodge a complaint** with your local data protection supervisory authority.

To exercise any of these rights, contact us via the channel in Section 9.

---

### 8. Children's Privacy

Tsundoku is not directed at children under 16, and we do not knowingly collect
personal data from them. If you believe a child has provided personal data,
please contact us so we can delete it.

---

### 9. International Users

The Tsundoku backend and your data are hosted in the European Union (Germany). If
you use the app from outside the EU, your data is processed on these EU servers.

---

### 10. Changes to This Policy

We may update this Privacy Policy from time to time. Material changes will be
reflected here with an updated "Last Updated" date.

---

### 11. Contact

If you have any questions about this policy or want to exercise your privacy
rights, reach out via the official GitHub repository.
