import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type {
  S3PresignedUrlRequest,
  S3PresignedUrlResponse,
  S3UploadImageResult,
} from '../types';

const MAX_IMAGE_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'webp']);
const ALLOWED_IMAGE_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

function getFileExtension(fileName: string) {
  const extensionIndex = fileName.lastIndexOf('.');
  if (extensionIndex < 0 || extensionIndex === fileName.length - 1) {
    return '';
  }

  return fileName.slice(extensionIndex + 1).toLowerCase();
}

export function getS3ImageFileValidationError(file: File): string | null {
  if (!file.name.trim()) {
    return 'Image file name is invalid.';
  }

  if (!ALLOWED_IMAGE_EXTENSIONS.has(getFileExtension(file.name))) {
    return 'Only JPG, PNG, and WebP images can be uploaded.';
  }

  if (!ALLOWED_IMAGE_CONTENT_TYPES.has(file.type)) {
    return 'Only JPEG, PNG, and WebP image formats are supported.';
  }

  if (file.size <= 0) {
    return 'Image file is empty.';
  }

  if (file.size > MAX_IMAGE_FILE_SIZE_BYTES) {
    return 'Image file must be 10MB or smaller.';
  }

  return null;
}

async function putFileToS3(uploadUrl: string, file: File) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  });

  if (!response.ok) {
    throw new Error(`S3 upload failed with status ${response.status}`);
  }
}

export function createS3Api(apiClient: AxiosInstance) {
  const createPresignedUrl = (payload: S3PresignedUrlRequest): Promise<S3PresignedUrlResponse> =>
    apiClient
      .post<ApiResponse<S3PresignedUrlResponse>>('/s3/presigned-url', payload)
      .then((response) => unwrapApiResponse(response.data));

  return {
    createPresignedUrl,

    uploadImage: async (file: File): Promise<S3UploadImageResult> => {
      const validationError = getS3ImageFileValidationError(file);
      if (validationError) {
        throw new Error(validationError);
      }

      const presignedUrl = await createPresignedUrl({
        fileName: file.name,
        contentType: file.type,
        fileSize: file.size,
      });

      await putFileToS3(presignedUrl.uploadUrl, file);

      return {
        objectKey: presignedUrl.objectKey,
      };
    },
  };
}

export type S3Api = ReturnType<typeof createS3Api>;
