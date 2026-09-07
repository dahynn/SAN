export interface S3PresignedUrlRequest {
  fileName: string;
  contentType: string;
  fileSize: number;
}

export interface S3PresignedUrlResponse {
  uploadUrl: string;
  objectKey: string;
  expiresInSeconds: number;
}

export interface S3UploadImageResult {
  objectKey: string;
}
