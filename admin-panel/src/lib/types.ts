export interface Widget {
  id?: string;
  name: string;
  description: string;
  categoryId: string;
  categoryName: string;
  thumbnailUrl: string;
  previewUrl: string;
  configUrl: string;
  configJson: string;
  isPro: boolean;
  isFeatured: boolean;
  isTrending: boolean;
  downloadCount: number;
  favoriteCount: number;
  tags: string[];
  version: number;
  active: boolean;
  createdAt?: number;
  updatedAt?: number;
  metadata?: Record<string, unknown>;
}

export interface Category {
  id?: string;
  name: string;
  icon: string;
  color: string;
  order: number;
  isActive: boolean;
  createdAt?: number;
}

export interface Theme {
  id?: string;
  name: string;
  description: string;
  thumbnailUrl: string;
  isPro: boolean;
  isDefault: boolean;
  config: Record<string, unknown>;
  active: boolean;
  createdAt?: number;
  updatedAt?: number;
}

export interface Announcement {
  id?: string;
  title: string;
  message: string;
  type: string;
  actionUrl: string;
  actionLabel: string;
  imageUrl: string;
  priority: number;
  startAt: number;
  endAt: number;
  active: boolean;
  createdAt?: number;
}

export interface Offer {
  id?: string;
  title: string;
  description: string;
  discountPercent: number;
  code: string;
  imageUrl: string;
  startAt: number;
  endAt: number;
  active: boolean;
  createdAt?: number;
}

export interface AppConfig {
  id?: string;
  latestVersion: number;
  forceUpdate: boolean;
  forceUpdateMessage: string;
  maintenanceMode: boolean;
  maintenanceMessage: string;
  minSupportedVersion: number;
  featureFlags: Record<string, boolean>;
  trendingWidgetIds: string[];
  featuredWidgetIds: string[];
  updatedAt?: number;
}
