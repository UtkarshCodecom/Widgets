# Firebase Firestore Schema

## Collections

### `widgets`
| Field | Type | Description |
|-------|------|-------------|
| `id` | string (auto) | Document ID |
| `name` | string | Widget display name |
| `description` | string | Widget description |
| `categoryId` | string | Reference to category |
| `categoryName` | string | Category display name |
| `thumbnailUrl` | string | Firebase Storage URL |
| `previewUrl` | string | Preview image URL |
| `configUrl` | string | Config file URL |
| `configJson` | string | Inline JSON config |
| `isPro` | boolean | Premium flag |
| `isFeatured` | boolean | Featured flag |
| `isTrending` | boolean | Trending flag |
| `downloadCount` | number | Download counter |
| `favoriteCount` | number | Favorite counter |
| `tags` | array<string> | Search tags |
| `version` | number | Widget version |
| `active` | boolean | Visibility flag |
| `createdAt` | timestamp | Creation time |
| `updatedAt` | timestamp | Last update |

### `categories`
| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Category name |
| `icon` | string | Icon identifier |
| `color` | string | Hex color |
| `order` | number | Sort order |
| `isActive` | boolean | Visibility |
| `createdAt` | timestamp | Creation time |

### `themes`
| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Theme name |
| `description` | string | Theme description |
| `thumbnailUrl` | string | Preview image |
| `isPro` | boolean | Premium flag |
| `isDefault` | boolean | Default flag |
| `config` | map<string, any> | Theme configuration |
| `active` | boolean | Visibility |
| `createdAt` | timestamp | Creation time |
| `updatedAt` | timestamp | Last update |

### `announcements`
| Field | Type | Description |
|-------|------|-------------|
| `title` | string | Announcement title |
| `message` | string | Announcement body |
| `type` | string | info/warning/update |
| `actionUrl` | string | CTA link |
| `actionLabel` | string | Button text |
| `imageUrl` | string | Banner image |
| `priority` | number | Display priority |
| `startAt` | timestamp | Start time |
| `endAt` | timestamp | End time |
| `active` | boolean | Visibility |

### `offers`
| Field | Type | Description |
|-------|------|-------------|
| `title` | string | Offer title |
| `description` | string | Offer description |
| `discountPercent` | number | Discount percentage |
| `code` | string | Promo code |
| `imageUrl` | string | Offer image |
| `startAt` | timestamp | Start time |
| `endAt` | timestamp | End time |
| `active` | boolean | Visibility |

### `appConfig` (single document: `config`)
| Field | Type | Description |
|-------|------|-------------|
| `latestVersion` | number | Latest version code |
| `forceUpdate` | boolean | Force update flag |
| `forceUpdateMessage` | string | Update prompt message |
| `maintenanceMode` | boolean | Maintenance flag |
| `maintenanceMessage` | string | Maintenance message |
| `minSupportedVersion` | number | Minimum version |
| `featureFlags` | map<string, boolean> | Feature toggles |
| `trendingWidgetIds` | array<string> | Trending widget IDs |
| `featuredWidgetIds` | array<string> | Featured widget IDs |

## Firebase Storage Structure

```
/widgets/
  thumbnails/
  previews/
  configs/
/themes/
  thumbnails/
/announcements/
/offers/
/uploads/
```

## Firebase Remote Config Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maintenance_mode` | `false` | Global maintenance |
| `force_update` | `false` | Force update toggle |
| `latest_version` | `1` | Latest version code |
