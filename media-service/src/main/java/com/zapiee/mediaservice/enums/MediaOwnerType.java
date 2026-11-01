package com.zapiee.mediaservice.enums;

/**
 * What a media asset is attached to. Menu images are the primary Cloudinary use case
 * today; logo/receipt are here because the File/Media Service in the platform blueprint
 * owns all three, and the storage abstraction (see StorageService) is provider-agnostic
 * so receipts could later move to a raw-PDF-friendly provider without touching callers.
 */
public enum MediaOwnerType {
    MENU_ITEM,
    MENU_CATEGORY,
    RESTAURANT_LOGO,
    RECEIPT,
    MENU_CATALOG,
    DOCUMENT
}
