# SecureFileVault – AES-256 Secure File Encryption System

## 1. Project Description

SecureFileVault is a Java-based cryptographic file encryption system that implements AES-256 symmetric encryption with secure password-based key derivation.

The system enables secure encryption and decryption of files using:
- Industry-standard AES-256
- PBKDF2 key derivation with HMAC-SHA256
- Secure random salt and initialization vector (IV)
- CBC mode with PKCS5 padding

This project demonstrates practical implementation of applied cryptography concepts, secure key management principles, and defensive security design practices.

The system is designed for educational and academic purposes to demonstrate secure file storage mechanisms and password-based encryption workflows.


---

## 2. Cryptographic Primitives Used

### Design & UI

**Web Interface Theme**
- Professional white-blue color scheme
- Gradient background (light blue to ultra-light blue)
- Clean card-based layout
- Google Material Icons throughout
- Smooth animations and transitions
- Fully responsive mobile design

**Web Interface Features**
- Real-time mode toggle (Encrypt/Decrypt)
- Drag-and-drop file upload
- Password visibility toggle
- Visual file selection feedback
- Loading spinner during processing
- Success/error message display
- One-click file download
- Cryptography information footer

---

## 2a. Cryptographic Primitives Used

### 2.1 AES-256 (Advanced Encryption Standard)
- Symmetric block cipher
- 256-bit key size
- 128-bit block size
- Operates in CBC (Cipher Block Chaining) mode
- Provides confidentiality of file data

### 2.2 PBKDF2WithHmacSHA256
- Password-Based Key Derivation Function
- Uses:
  - HMAC-SHA256
  - 65,536 iterations
  - 16-byte cryptographic salt
- Protects against brute-force and rainbow table attacks
- Converts user password into strong 256-bit AES key

### 2.3 SHA-256 (within PBKDF2)
- Cryptographic hash function
- Provides collision resistance and preimage resistance
- Used internally for secure key stretching

### 2.4 SecureRandom
- Cryptographically strong pseudo-random number generator
- Generates:
  - Salt
  - Initialization Vector (IV)

### 2.5 AES/CBC/PKCS5Padding
- CBC mode prevents identical ciphertext patterns
- PKCS5 padding ensures proper block alignment


---

## 3. How to Compile

### Requirements:
- Java JDK 8 or higher
- Command line access

Navigate to the project directory:

```
cd SecureFileVault
```

Compile both source files:

```
javac SecureFileVault.java SecureFileVaultGUI.java
```

This will generate:

```
SecureFileVault.class
SecureFileVaultGUI.class
```

---

## 4. Runtime Modes

### Mode 1: Web Interface (Recommended) 🌐

Start a local web server with a beautiful, modern UI:

```bash
java SecureFileVault web
```

Open your browser to `http://localhost:8080` and encrypt/decrypt files with a polished **white-blue themed interface**.

**Features:**
- Clean card-based layout with gradient background
- Real-time Encrypt/Decrypt mode toggle
- Drag-and-drop file upload
- Google Material Icons
- Real-time validation feedback
- Smooth animations & transitions
- Mobile-responsive design
- One-click file download
- Password visibility toggle
- Processing spinner feedback

---

### Mode 2: Desktop GUI (Offline) 🖥️

Launch the Swing-based graphical interface:

```bash
java SecureFileVault
```

**Features:**
- Drag-and-drop file input
- Password visibility toggle
- Real-time status bar
- Progress indicator
- Responsive background processing

---

### Mode 3: Command-line (Scripts) 💻

For integration with scripts or batch operations:

```bash
java SecureFileVault encrypt <filename> <password>
java SecureFileVault decrypt <filename> <password>
```

**Examples:**

```bash
java SecureFileVault encrypt document.pdf secretpass
java SecureFileVault decrypt document.pdf.enc secretpass
```

---

## 5. How to Run

The application supports two modes of operation:

1. **Command-line** (original behavior)
2. **Graphical user interface** (new frontend)

### Command‑line Usage

#### Encrypt a File

```
java SecureFileVault encrypt <filename> <password>
```

Example:

```
java SecureFileVault encrypt example.txt mySecurePassword
```

Output:
- Encrypted file will be created as: `example.txt.enc`

---

#### Decrypt a File

```
java SecureFileVault decrypt <filename.enc> <password>
```

Example:

```
java SecureFileVault decrypt example.txt.enc mySecurePassword
```

Output:
- Decrypted file will be created as: `decrypted_example.txt`

---

### Graphical Interface (Swing)

If you run the program without any arguments, the Swing-based GUI will launch.
The interface has been greatly improved for usability and interactivity:

- **Drag & drop** a file onto the path field or use the browse button.
- Password box with **show/hide toggle** (click the checkbox).
- Real-time **status bar** and **indeterminate progress indicator** during operations.
- Buttons are disabled while work is running to prevent accidental clicks.

To launch the GUI:

```
java SecureFileVault
```

The same `.class` files are used for both modes; just compile both
`SecureFileVault.java` and `SecureFileVaultGUI.java`.

> 💡 You can also drag `*.enc` files into the window when decrypting.

---

### Web Interface (Enhanced UI)

A modern, responsive web frontend with a professional **white-blue theme** is now available.
It runs in your browser and provides a polished, production-ready interface.

**Features:**

- Clean card-based layout with gradient background
- Encrypt/Decrypt mode toggle
- Drag-and-drop file upload
- Google Material Icons integration
- Real-time validation feedback
- Smooth animations and transitions
- Mobile-responsive design
- One-click download of processed files
- Password visibility toggle
- Loading spinner feedback

Start the server:

```
java SecureFileVault web
```

Then navigate to:

```
http://localhost:8080
```

The interface includes:

1. **Mode Selector**: Toggle between Encrypt and Decrypt
2. **File Upload**: Select or drag-and-drop files
3. **Password Field**: Enter encryption/decryption password
4. **Action Button**: Processes the file in the background
5. **Status Messages**: Real-time feedback on operations

All processing happens client-side in the browser; files are transferred to the
local server via base64 encoding and never leave your machine.

> **Tip**: You can refresh the page and process another file without restarting the server.

---

#### Notes on source control

A `.gitignore` file is included to avoid committing build artifacts and
sensitive output. It already ignores:

- compiled `.class`, `.jar`, `.war`, etc.
- encrypted files (`*.enc`) and decrypted outputs (`decrypted_*`)
- IDE metadata, OS files, temporary files

You may add additional ignores if you generate other resources.
#### Notes on source control

A `.gitignore` file is included to avoid committing build artifacts and
sensitive output. It already ignores:

- compiled `.class`, `.jar`, `.war`, etc.
- encrypted files (`*.enc`) and decrypted outputs (`decrypted_*`)
- IDE metadata, OS files, temporary files

You may add additional ignores if you generate other resources.

---

## 5. Security Design

### 5.1 Key Derivation Security

- Raw passwords are NEVER used directly as encryption keys.
- PBKDF2 is used with:
- 65,536 iterations
- Unique 16-byte salt per encryption
- This increases computational cost of brute-force attacks.

---

### 5.2 Salt Handling

- A new cryptographically secure random salt is generated for every encryption.
- Salt is prepended to the encrypted file.
- Format of encrypted file:


[SALT (16 bytes)] [IV (16 bytes)] [CIPHERTEXT]


---

### 5.3 Initialization Vector (IV)

- Random 16-byte IV generated per encryption.
- Ensures semantic security.
- Prevents identical plaintext blocks from producing identical ciphertext blocks.

---

### 5.4 Confidentiality

- AES-256 in CBC mode ensures data confidentiality.
- PKCS5 padding ensures secure block alignment.

---

### 5.5 Attack Resistance

This system provides protection against:

- Brute-force attacks (via PBKDF2 key stretching)
- Rainbow table attacks (via random salt)
- Pattern leakage (via CBC mode + random IV)

---

### 5.6 Limitations (Academic Transparency)

- Does not implement authenticated encryption (e.g., AES-GCM).
- Does not include integrity verification (no HMAC or MAC tag).
- Designed as an academic cryptography demonstration project.

Future enhancement:
- Upgrade to AES-GCM for authenticated encryption.
- Add HMAC-SHA256 for integrity verification.
- Implement hybrid encryption (RSA + AES).

---

## 6. Academic Learning Outcomes

This project demonstrates understanding of:

- Symmetric encryption systems
- Secure key derivation
- Cryptographic randomness
- Secure storage architecture
- Practical implementation of cryptographic APIs in Java

---

## 7. Disclaimer

This project is intended for academic and educational use only. It should not be deployed in production environments without additional security auditing and authenticated encryption mechanisms.