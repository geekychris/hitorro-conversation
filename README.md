<p align="center">
  <img src="assets/logo.png" alt="Hitorro Conversation logo" width="180">
</p>

# Hitorro Conversation

This is the **hitorro-conversation** project, providing conversation tracking, link analysis, and thread discovery for forum posts, blog comments, and social media discussions.

## Overview

The Conversation module analyzes relationships between posts, comments, and discussions to identify conversation threads, track participation, and generate RSS feeds of related content. It uses link analysis algorithms to find connections and rank conversations by various metrics.

## Features

### Link Engine

- **Link Analysis**: Discover relationships between posts based on content similarity, citations, and replies
- **Graph-Based Storage**: Efficient RLE (Run-Length Encoded) storage for link graphs
- **Tuple-Based Queries**: Fast querying of link relationships
- **Memory-Efficient**: Compressed storage for large conversation graphs
- **Segment Management**: Partitioned storage for scalability

### Conversation Tracking

- **Thread Discovery**: Automatically identify conversation threads
- **Participation Analysis**: Track who's involved in conversations
- **Ranking Algorithms**: Score conversations by relevance, participation, and activity
- **Temporal Analysis**: Track conversation evolution over time
- **Spam Removal**: Filter out spam and low-quality discussions

### RSS Feed Generation

- **Multiple Algorithms**: Different strategies for feed generation
  - Conversation-based feeds
  - Post-based feeds
  - Bookmark feeds
  - Similarity-based feeds
  - Shared content feeds
- **Filtering**: Content and quality filtering
- **Pagination**: Support for paginated feed results

## Project Structure

```
hitorro-conversation/
├── pom.xml
├── README.md
└── src/main/java/com/hitorro/
    ├── analysis/linkengine/         - Link analysis engine
    │   ├── compute/                 - Computation algorithms
    │   │   ├── ComputeConversation.java
    │   │   ├── ConversationRanker.java
    │   │   ├── MemoryConversation.java
    │   │   ├── RankByParticipantSpread.java
    │   │   └── SpamRemovalRanker.java
    │   ├── tuple/                   - Tuple-based storage
    │   │   ├── RLELinkTupleReader.java
    │   │   ├── RLELinkTupleWriter.java
    │   │   ├── TupleReaderFilter.java
    │   │   └── filters/
    │   ├── IndexLink.java
    │   ├── LinkEngine.java
    │   ├── LinkSegment.java
    │   └── TuplePool.java
    ├── conversationtracker/         - Conversation tracking
    │   ├── feeds/                   - Feed generation
    │   ├── rss/                     - RSS feed algorithms
    │   │   ├── algorithms/
    │   │   │   ├── ConversationAlgorithm.java
    │   │   │   ├── PostAlgorithm.java
    │   │   │   ├── BookmarksAlgorithm.java
    │   │   │   ├── SimilarAlgorithm.java
    │   │   │   └── SharedAlgorithm.java
    │   │   ├── RssFeedGenerator.java
    │   │   └── RssIteratorGenerator.java
    │   ├── Conversation.java
    │   ├── ConversationQuery.java
    │   └── ConversationTrackerService.java
    ├── basedms/                     - DMS integration
    │   └── xml/
    │       ├── RssWriter.java
    │       └── XWriter.java
    ├── basetext/commands/           - Text commands
    │   └── SimilarityCommand.java
    └── sampleapp/                   - Sample application
        ├── ConversationCalculationJob.java
        └── ConversationCalculationParameters.java
```

**Total: 85 Java source files**

## Dependencies

### Internal Hitorro Dependencies (provided scope)
- `hitorro-text-core` (3.0.0) - Text processing
- `hitorro-text-persistence` (3.0.0) - Text indexing and search
- `hitorro-basedms` (3.0.0) - Database persistence

### External Dependencies
- **Trove4j** (3.0.3) - High-performance collections
- **ROME** (2.1.0) - RSS/Atom feed processing
- **Apache Commons Lang3** (3.14.0) - String utilities

## Building

### Build the JAR

```bash
cd /Users/chris/hitorro/hitorro-conversation
mvn clean package
```

This will create:
- `target/hitorro-conversation-3.0.0.jar` - Standard JAR
- `target/hitorro-conversation-3.0.0-jar-with-dependencies.jar` - Fat JAR with all dependencies

### Install to Local Repository

```bash
mvn clean install
```

### Build Without Tests

```bash
mvn clean package -DskipTests
```

## Usage Examples

### Analyze Conversations

```java
import com.hitorro.conversationtracker.ConversationTrackerService;
import com.hitorro.conversationtracker.Conversation;

// Initialize service
ConversationTrackerService service = new ConversationTrackerService();

// Compute conversations from posts
List<Conversation> conversations = service.computeConversations(posts);

// Rank by participation
conversations.sort(new RankByParticipantSpread());

// Get top conversations
List<Conversation> top10 = conversations.subList(0, 10);
```

### Generate RSS Feeds

```java
import com.hitorro.conversationtracker.rss.RssFeedGenerator;
import com.hitorro.conversationtracker.rss.algorithms.ConversationAlgorithm;

// Create feed generator
RssFeedGenerator generator = new RssFeedGenerator();
generator.setAlgorithm(new ConversationAlgorithm());

// Generate feed
String rssFeed = generator.generateFeed(conversations, 20);

// Write to file
Files.writeString(Path.of("conversations.xml"), rssFeed);
```

### Link Engine Operations

```java
import com.hitorro.analysis.linkengine.LinkEngine;
import com.hitorro.analysis.linkengine.IndexLink;

// Initialize link engine
LinkEngine engine = new LinkEngine(indexPath);

// Add links
engine.addLink(new IndexLink(postId1, postId2, linkType, timestamp));

// Query links
Iterator<IndexLink> links = engine.queryLinks(postId1);
while (links.hasNext()) {
    IndexLink link = links.next();
    // Process link relationship
}
```

### Conversation Ranking

```java
import com.hitorro.analysis.linkengine.compute.ConversationRanker;
import com.hitorro.analysis.linkengine.compute.SpamRemovalRanker;

// Create ranking pipeline
ConversationRanker ranker = new ConversationRanker();
ranker.addRanker(new SpamRemovalRanker());
ranker.addRanker(new RankByParticipantSpread());

// Rank conversations
List<Conversation> ranked = ranker.rank(conversations);
```

## Link Storage Format

The link engine uses RLE (Run-Length Encoded) compression for efficient storage:

```
[docId1][linkType][count][docId2][docId3]...[docIdN]
```

Benefits:
- **Compression**: 70-90% size reduction compared to uncompressed
- **Fast Queries**: Direct access without full decompression
- **Scalability**: Handles millions of links efficiently

## RSS Feed Algorithms

### Conversation Algorithm
Groups related posts into conversations and generates feeds based on conversation activity.

### Post Algorithm
Simple chronological feed of individual posts.

### Bookmarks Algorithm
Feeds based on user bookmarks and saved items.

### Similarity Algorithm
Finds and groups similar posts based on content similarity.

### Shared Algorithm
Tracks shared or referenced content across posts.

## Configuration

```properties
# Link Engine Configuration
linkengine.index.path=/data/conversations/index
linkengine.segment.size=1000000
linkengine.compression.enabled=true

# Conversation Tracker
conversation.min.posts=3
conversation.max.age.days=30
conversation.spam.threshold=0.7

# RSS Feed Generation
rss.feed.size=50
rss.feed.title=Conversations
rss.feed.description=Tracked conversations and discussions
```

## Performance

- **Link Indexing**: ~50K links/second
- **Query Speed**: <10ms for typical queries
- **Memory Usage**: ~100MB for 1M links (compressed)
- **Feed Generation**: ~100 conversations/second

## Testing

```bash
mvn test
```

## Requirements

- Java 19 or higher
- Maven 3.6+
- Sufficient disk space for link index storage

## Maven Artifact

To use this module in other Maven projects:

```xml
<dependency>
    <groupId>ht</groupId>
    <artifactId>hitorro-conversation</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Algorithm Details

### Link Analysis
The link engine uses a graph-based approach to identify relationships:
1. **Content Similarity**: TF-IDF based similarity scoring
2. **Citation Analysis**: Explicit links and references
3. **Temporal Proximity**: Posts close in time are more likely related
4. **Author Relationships**: Track reply patterns

### Ranking Metrics
- **Participant Spread**: More diverse participants = higher score
- **Activity Level**: Recent activity boosts ranking
- **Content Quality**: Spam filtering and quality signals
- **Engagement**: Replies, reactions, shares

## License

See project root for license information.
