#!/bin/bash
POST_ID=2
BOT_ID=1
URL="http://localhost:8080/api/posts/$POST_ID/comments"
for i in $(seq 1 200); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$URL" -H "Content-Type: application/json" -d "{\"authorId\": $BOT_ID, \"authorType\": \"BOT\", \"content\": \"bot comment $i\", \"depthLevel\": 1}" &
done
wait
echo "ALL DONE"
