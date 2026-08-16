<div align="center">

# 🎬 ByteCast

### A video streaming backend built with Java, Quarkus, FFmpeg & HLS

<p>
  <strong>Upload</strong> · <strong>Process</strong> · <strong>Transcode</strong> · <strong>Stream</strong>
</p>

<p>
  <img src="https://img.shields.io/badge/Java-17+-orange" alt="Java">
  <img src="https://img.shields.io/badge/Quarkus-3.x-blue" alt="Quarkus">
  <img src="https://img.shields.io/badge/FFmpeg-enabled-green" alt="FFmpeg">
  <img src="https://img.shields.io/badge/HLS-supported-purple" alt="HLS">
  <img src="https://img.shields.io/badge/MinIO-object%20storage-red" alt="MinIO">
</p>

<p>
  <a href="#-overview">Overview</a> ·
  <a href="#-features">Features</a> ·
  <a href="#-architecture">Architecture</a> ·
  <a href="#-getting-started">Getting Started</a> ·
  <a href="#-api">API</a> ·
  <a href="#-players">Players</a>
</p>

</div>

---

## ✨ Overview

**ByteCast** is a video streaming backend built with **Java and Quarkus**.

It provides a complete pipeline for uploading, processing, storing, transcoding, and streaming videos using **MinIO**, **FFmpeg**, and **HLS**.

The project is designed around a simple principle:

> **Upload the video directly to object storage, process it asynchronously, and stream it at multiple qualities.**

ByteCast keeps large file transfers and long-running video processing outside the normal HTTP request lifecycle, providing a foundation for a scalable video streaming platform.

---

## 🚀 Features

|     | Feature                | Description                                                   |
| --- | ---------------------- | ------------------------------------------------------------- |
| 📤  | **Direct Uploads**     | Upload videos directly to MinIO using temporary POST policies |
| 🗄️ | **Object Storage**     | Store source videos and generated HLS content in MinIO        |
| ⚙️  | **FFmpeg Processing**  | Transcode videos using the FFmpeg command-line interface      |
| 🔄  | **Batch Jobs**         | Process videos asynchronously using background jobs           |
| 🎚️ | **Multi-Quality HLS**  | Generate multiple resolutions and bitrates                    |
| 📑  | **HLS Playlists**      | Generate master and quality-specific playlists                |
| 🧩  | **HLS Segments**       | Serve generated video segments                                |
| 📡  | **Adaptive Streaming** | Support switching between available quality variants          |
| 🎛️ | **Quality Selection**  | Allow the player to select a specific quality                 |
| 🎬  | **Web Players**        | Includes range, HLS and multi-quality player examples         |

---

# 🏗️ Architecture

ByteCast separates **uploading, storage, processing, and streaming** into different stages.

```text
                         ┌──────────────────────┐
                         │        Client        │
                         │   Web / Application  │
                         └──────────┬───────────┘
                                    │
                                    │ Request upload policy
                                    ▼
                         ┌──────────────────────┐
                         │      ByteCast        │
                         │      Quarkus API     │
                         └──────────┬───────────┘
                                    │
                                    │ Generate POST Policy
                                    ▼
                         ┌──────────────────────┐
                         │        Client        │
                         └──────────┬───────────┘
                                    │
                                    │ Multipart POST
                                    ▼
                         ┌──────────────────────┐
                         │        MinIO         │
                         │    Object Storage    │
                         └──────────┬───────────┘
                                    │
                                    │ Source Video
                                    ▼
                         ┌──────────────────────┐
                         │      Batch Job       │
                         │       Worker         │
                         └──────────┬───────────┘
                                    │
                                    │ FFmpeg
                                    ▼
                         ┌──────────────────────┐
                         │       FFmpeg         │
                         │    HLS Transcoding   │
                         └──────────┬───────────┘
                                    │
                   ┌────────────────┼────────────────┐
                   │                │                │
                   ▼                ▼                ▼
                1080p             720p             480p
                   │                │                │
                   └────────────────┼────────────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     HLS Output       │
                         │ master + playlists   │
                         │      + segments      │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │        Player        │
                         │     HLS / HLS.js     │
                         └──────────────────────┘
```

---

## 📤 Direct Upload with POST Policies

ByteCast uses **MinIO POST policies** to allow clients to upload videos directly to object storage.

Instead of sending large video files through the ByteCast API, the client first requests a temporary upload policy.

ByteCast generates the policy and returns the required fields to the client.

The client then performs a multipart `POST` directly to MinIO.

```text
Client
  │
  │  Request upload policy
  ▼
ByteCast API
  │
  │  Generate temporary POST policy
  ▼
Client
  │
  │  Multipart POST
  ▼
MinIO
```

### Upload Flow

```text
1. Client requests an upload policy
                │
                ▼
2. ByteCast generates a temporary POST policy
                │
                ▼
3. Client receives the policy fields
                │
                ▼
4. Client uploads the video directly to MinIO
                │
                ▼
5. MinIO stores the source video
                │
                ▼
6. Client starts the processing job
```

### Why POST Policies?

Direct uploads provide several advantages:

* 🚀 Large files bypass the application server
* 📈 Reduces network and memory pressure on the API
* 💾 Videos don't need to pass through ByteCast
* 🔐 Upload constraints can be enforced through the policy
* ⏱️ Policies can expire after a configured period
* 📦 MinIO handles the actual object upload

---

# 🗄️ Object Storage

ByteCast uses **MinIO** as its blob/object storage layer.

MinIO stores both the original source videos and the generated HLS content.

A typical storage layout can look like:

```text
videos/
├── source/
│   └── {video-id}/
│       └── video.mp4
│
└── hls/
    └── {video-id}/
        ├── master.m3u8
        │
        ├── 1080p/
        │   ├── playlist.m3u8
        │   ├── segment_000.ts
        │   ├── segment_001.ts
        │   └── ...
        │
        ├── 720p/
        │   ├── playlist.m3u8
        │   └── ...
        │
        ├── 480p/
        │   ├── playlist.m3u8
        │   └── ...
        │
        └── 360p/
            ├── playlist.m3u8
            └── ...
```

Keeping media files in object storage allows the application layer to remain lightweight and makes it easier to scale the API independently from media storage.

---

# 🔄 Batch Video Processing

Video transcoding is a long-running operation, so ByteCast handles it through a **background batch job**.

The API can create a processing job without keeping the HTTP request open while FFmpeg runs.

```text
Create Job
    │
    ▼
Download Source
    │
    ▼
Create Temporary Workspace
    │
    ▼
Run FFmpeg
    │
    ▼
Generate HLS Variants
    │
    ▼
Upload HLS Output
    │
    ▼
Update Job Status
```

A job can track its lifecycle from creation through completion or failure.

This architecture provides a foundation for:

* Multiple concurrent video jobs
* Retry mechanisms
* Job monitoring
* Distributed processing workers
* Long-running video operations

---

# 🎞️ FFmpeg Processing

ByteCast uses the **FFmpeg command-line interface** for video transcoding.

The source video is converted into multiple HLS variants.

Each variant can define its own:

* Resolution
* Video bitrate
* Audio bitrate
* HLS playlist
* HLS segments

For example:

```text
                    Original Video
                          │
                          ▼
                       FFmpeg
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
           1080p         720p         480p
             │            │            │
             ▼            ▼            ▼
        playlist.m3u8 playlist.m3u8 playlist.m3u8
             │            │            │
             ▼            ▼            ▼
          segments     segments     segments
```

The generated variants are then combined through an HLS master playlist.

---

# 📡 Multi-Quality HLS

ByteCast generates a master playlist containing multiple quality variants.

```text
master.m3u8
     │
     ├── 1080p/playlist.m3u8
     ├── 720p/playlist.m3u8
     ├── 480p/playlist.m3u8
     └── 360p/playlist.m3u8
```

Each variant has its own bitrate and resolution.

For example:

```text
1080p  ── 5000 kbps
720p   ── 3000 kbps
480p   ── 1500 kbps
360p   ── 800 kbps
```

An HLS-compatible player can use the master playlist to discover the available variants.

The player can then select an appropriate quality and, when supported, switch between variants as network conditions change.

This provides the foundation for **adaptive bitrate streaming**.

---

# 📂 HLS Output

A processed video produces an HLS structure similar to:

```text
hls/
├── master.m3u8
│
├── 1080p/
│   ├── playlist.m3u8
│   ├── segment_000.ts
│   ├── segment_001.ts
│   └── ...
│
├── 720p/
│   ├── playlist.m3u8
│   ├── segment_000.ts
│   └── ...
│
├── 480p/
│   ├── playlist.m3u8
│   └── ...
│
└── 360p/
    ├── playlist.m3u8
    └── ...
```

The `master.m3u8` file acts as the entry point for playback.

---

# 🎬 Players

ByteCast includes lightweight HTML players for testing the streaming pipeline.

### 📦 Range Player

Demonstrates standard HTTP range-based video playback using the HTML5 `<video>` element.

### 📡 HLS Player

Demonstrates HLS playback using **HLS.js**.

### 🎚️ Multi-Quality Player

Demonstrates playback through the HLS master playlist and provides quality selection.

These players make it possible to test the backend without building a separate frontend application.

---

# 🔌 API

ByteCast exposes REST endpoints for managing uploads, processing jobs, and serving HLS content.

## Upload

The client first requests a temporary MinIO POST policy.

```http
POST /...
```

The response contains the information required for the client to upload the video directly to MinIO.

## Processing

After the upload is complete, a processing job can be started.

```http
POST /...
```

The batch worker then retrieves the source video and starts the FFmpeg conversion.

## HLS Master Playlist

Returns the master HLS playlist.

```http
GET /...
```

## Quality Playlist

Returns the playlist for a specific quality.

```http
GET /...
```

## HLS Segment

Returns an individual HLS media segment.

```http
GET /...
```

> The exact endpoint paths and parameters are defined by the REST resources in the project.

---

# 🛠️ Getting Started

## Prerequisites

Make sure you have the following installed:

* **Java 17+**
* **Maven**
* **Docker**
* **FFmpeg**
* **MinIO**

## Clone the Repository

```bash
git clone https://github.com/haithembenkhalef/video-streaming.git

cd video-streaming
```

## Start MinIO

Run MinIO locally using Docker:

```bash
docker run \
  -p 9000:9000 \
  -p 9001:9001 \
  minio/minio \
  server /data \
  --console-address ":9001"
```

MinIO will be available at:

```text
API:     http://localhost:9000
Console: http://localhost:9001
```

## Run ByteCast

Start the application in Quarkus development mode:

```bash
./mvnw quarkus:dev
```

The application will be available at:

```text
http://localhost:8080
```

---

# ⚙️ Configuration

Configure the MinIO connection through Quarkus configuration.

Example:

```properties
quarkus.minio.endpoint=http://localhost:9000
quarkus.minio.access-key=minioadmin
quarkus.minio.secret-key=minioadmin
```

> Configuration property names may change as the project evolves. Check the application configuration for the current values.

---

# 🧰 Tech Stack

| Technology     | Purpose                       |
| -------------- | ----------------------------- |
| ☕ **Java**     | Backend language              |
| ⚡ **Quarkus**  | Application framework         |
| 🗄️ **MinIO**  | Object/blob storage           |
| 🎞️ **FFmpeg** | Video transcoding             |
| 📡 **HLS**     | HTTP Live Streaming           |
| 🌐 **HLS.js**  | Browser HLS playback          |
| 📦 **Maven**   | Build & dependency management |

---

# 🗺️ Roadmap

ByteCast currently focuses on the core video processing and streaming pipeline.

Potential future improvements include:

* [ ] Improved job monitoring
* [ ] Processing progress reporting
* [ ] More encoding profiles
* [ ] Automatic quality selection improvements
* [ ] Authentication & authorization
* [ ] Video metadata management
* [ ] Upload progress tracking
* [ ] Distributed processing workers
* [ ] Scalable media delivery

---

# 🤝 Contributing

Contributions, ideas, and improvements are welcome.

Feel free to open an issue or submit a pull request.

---

# 📄 License

See the repository for license information.

---

<div align="center">

## 🎬 ByteCast

**From direct upload to adaptive HLS streaming.**

Built with ❤️ using **Java · Quarkus · MinIO · FFmpeg · HLS**

</div>
