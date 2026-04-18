#!/bin/sh
set -e

echo "### Seeding MinIO Demo Assets ###"

CONTAINER="caries-minio-init"
FILES="demo_assets/pano_CA-2026-LOW.jpg demo_assets/peri1_CA-2026-LOW.jpg demo_assets/pano_CA-2026-HIGH.jpg demo_assets/peri1_CA-2026-HIGH.jpg demo_assets/peri2_CA-2026-HIGH.jpg"

# 1x1 JPEG in base64
B64_1x1_JPG="/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="

for FILE in $FILES; do
    echo "Uploading mock image to $FILE..."
    # Creating the dummy image in tmp and using 'mc cp' to upload
    CMD="echo '$B64_1x1_JPG' | base64 -d > /tmp/dummy.jpg && mc cp /tmp/dummy.jpg caries/caries-image/$FILE > /dev/null"
    
    docker exec -i $CONTAINER sh -c "$CMD"
done

echo "Demo assets seeded successfully!"