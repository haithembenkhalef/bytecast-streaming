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
  <a href="#-api">API</a> ·
  <a href="#-getting-started">Getting Started</a>
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

|     | Feature                 | Description                                                              |
| --- | ----------------------- | ------------------------------------------------------------------------ |
| 📤  | **Direct Uploads**      | Upload videos directly to MinIO using temporary pre-signed POST policies |
| 🗄️ | **Object Storage**      | Store source videos and generated HLS content in MinIO                   |
| ⚙️  | **FFmpeg Processing**   | Transcode videos using the FFmpeg command-line interface                 |
| 🔄  | **Batch Jobs**          | Process videos asynchronously using background jobs                      |
| 🎚️ | **Multi-Quality HLS**   | Generate multiple resolutions and bitrates                               |
| 📑  | **HLS Master Playlist** | Expose a master playlist containing available qualities                  |
| 🧩  | **HLS Segments**        | Serve generated video segments                                           |
| 📡  | **Adaptive Streaming**  | Support switching between available quality variants                     |
| 🎛️ | **Quality Selection**   | Allow the player to select a specific quality                            |
| 🎬  | **Web Players**         | Includes range, HLS and multi-quality player examples                    |

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

## 📤 Direct Upload with Pre-Signed POST Policies

ByteCast uses **MinIO pre-signed POST policies** to allow clients to upload videos directly to object storage.

Instead of sending a large video file through the ByteCast API, the client first requests an upload policy.

The API returns:

* The MinIO upload URL
* The form fields required by the POST policy

The client then performs a `multipart/form-data` POST directly to MinIO.

```text
Client
  │
  │ GET /api/videos/{videoId}/upload
  ▼
ByteCast API
  │
  │ Generate POST policy
  ▼
Client
  │
  │ multipart/form-data POST
  ▼
MinIO
  │
  │ Video stored
  ▼
Object Storage
```

### Upload Flow

```text
1. Create / identify a video
              │
              ▼
2. Request upload policy
              │
              ▼
3. ByteCast generates temporary POST policy
              │
              ▼
4. Client receives URL + formData
              │
              ▼
5. Client uploads directly to MinIO
              │
              ▼
6. Start HLS processing job
```

This approach keeps large file uploads away from the application server.

### Benefits

* 🚀 Large files bypass the application server
* 📈 Reduces network and memory pressure on the API
* 💾 Videos don't need to pass through ByteCast
* 🔐 Upload constraints can be enforced through the policy
* ⏱️ Policies can expire after a configured period
* 📦 MinIO handles the actual object upload

---

# 🗄️ Object Storage

ByteCast uses **MinIO** as its blob/object storage layer.

MinIO stores the original source videos as well as the generated HLS content.

A typical output structure looks like:

```text
videos/
└── {videoId}/
    │
    ├── source/
    │   └── video.mp4
    │
    └── hls/
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

Keeping media files in object storage allows the application layer to remain lightweight and makes it possible to scale the API independently from media storage.

---

# 🔄 Batch Video Processing

Video transcoding is a long-running operation, so ByteCast handles it through a **background processing job**.

The processing endpoint starts the job and returns a `VideoProcessingJob` containing its current status.

```text
Create Job
    │
    ▼
PENDING
    │
    ▼
RUNNING
    │
    ▼
Download Source
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
COMPLETED
```

A processing job contains:

| Field          | Description                             |
| -------------- | --------------------------------------- |
| `id`           | Unique job identifier                   |
| `videoId`      | Video being processed                   |
| `status`       | Current processing status               |
| `createdAt`    | Job creation timestamp                  |
| `startedAt`    | Processing start timestamp              |
| `finishedAt`   | Processing completion timestamp         |
| `errorMessage` | Error information when processing fails |

Current job states include:

```text
PENDING
RUNNING
COMPLETED
```

This architecture keeps long-running FFmpeg operations outside the HTTP request lifecycle and provides a foundation for future job monitoring, retries, and distributed workers.

---

# 🎞️ FFmpeg Processing

ByteCast uses the **FFmpeg command-line interface** for video transcoding.

The source video is converted into multiple HLS variants.

Each variant can have its own:

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

The generated variants are combined through an HLS master playlist.

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

An HLS-compatible player starts from the master playlist and discovers the available variants.

The player can then select a specific quality or switch between variants depending on its playback strategy and network conditions.

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

# 🔌 API

Base path:

```text
/api/videos
```

## 📤 Generate Upload Policy

Generate a temporary MinIO POST policy for uploading a video.

```http
GET /api/videos/{videoId}/upload
```

### Response

```json
{
  "url": "http://localhost:9000/...",
  "formData": {
    "key": "...",
    "policy": "...",
    "x-amz-algorithm": "...",
    "x-amz-credential": "...",
    "x-amz-date": "...",
    "x-amz-signature": "..."
  }
}
```

The client uses the returned `url` and `formData` to perform the actual multipart upload directly to MinIO.

---

## ⚙️ Start HLS Processing

Start the HLS generation job for a video.

```http
POST /api/videos/{videoId}/HlsGenerationJob
Content-Type: application/json
```

The endpoint returns a `VideoProcessingJob`.

Example:

```json
{
  "id": "job-123",
  "videoId": "video-123",
  "status": "PENDING",
  "createdAt": "2026-08-16T18:00:00Z",
  "startedAt": null,
  "finishedAt": null,
  "errorMessage": null
}
```

---

## 📑 Get HLS Master Playlist

Returns the master HLS playlist.

```http
GET /api/videos/{videoId}/hls/master.m3u8
```

Content type:

```text
application/vnd.apple.mpegurl
```

The master playlist references the available quality variants.

---

## 🎚️ Get Quality Segment

Returns a segment from a specific quality.

```http
GET /api/videos/{videoId}/hls/{quality}/{segment}
```

Example:

```http
GET /api/videos/123/hls/720p/segment_001.ts
```

Content type:

```text
video/mp2t
```

---

## 🧩 Get Segment

A segment can also be requested without explicitly specifying the quality.

```http
GET /api/videos/{videoId}/hls/{segment}
```

Content type:

```text
video/mp2t
```

---

## ▶️ Stream Original Video

ByteCast also supports standard HTTP video streaming.

```http
GET /api/videos/{videoId}/stream
```

The endpoint accepts the HTTP `Range` header, allowing clients to request portions of the original video.

Example:

```http
Range: bytes=0-1048575
```

This allows standard HTML5 video playback and seeking.

---

# 🎬 Players

The repository includes lightweight HTML players for testing the streaming pipeline.

### 📦 Range Player

Demonstrates standard HTTP range-based video playback using the HTML5 `<video>` element.

### 📡 HLS Player

Demonstrates HLS playback using **HLS.js**.

### 🎚️ Multi-Quality Player

Demonstrates playback using the HLS master playlist with quality selection.

These examples make it possible to test the backend without building a separate frontend application.

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

* [ ] Job monitoring and progress reporting
* [ ] Job retry mechanisms
* [ ] More encoding profiles
* [ ] Improved automatic quality selection
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
