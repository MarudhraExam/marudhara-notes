import express from 'express';
import path from 'path';
import fs from 'fs';

const app = express();
const port = 3000;

// Serve static assets from public folder
app.use(express.static(path.join(__dirname, 'public')));

// Serve the compiled Android APK
app.get('/app-debug.apk', (req, res) => {
  const apkPath = path.join(__dirname, '.build-outputs', 'app-debug.apk');
  if (fs.existsSync(apkPath)) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="marudhara-exam.apk"');
    res.sendFile(apkPath);
  } else {
    res.status(404).send('APK is still building. Please refresh in a moment.');
  }
});

// For any other route, fallback to index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(port, () => {
  console.log(`Preview server running on port ${port}`);
});
