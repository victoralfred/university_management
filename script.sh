for i in {1..5}; do
  curl -X POST "http://localhost:8090/api/v1/process/enqueue/${i}/$((i + 10))"
done