# Read Me First

Modify `src/main/resources/application.properties` to set your 
values or override via ENV Variables

### Running
`./mvnw spring-boot:run`

Open browser to `http://localhost:7100/`

Select a file to upload, this will fetch two URLs:
1) A presigned S3 URL for a regular region endpoint
2) A presigned S3 URL for the transfer acceleration endpoint

Both transfer times will be logged in the browser console.

A significant difference appears when the client browser is not 
geographically located in the same region as the S3 bucket.