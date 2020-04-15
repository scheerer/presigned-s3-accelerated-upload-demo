package blendedsoftware.aws.presigneds3accelerateduploaddemo;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootApplication
@Controller
@Slf4j
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}


	@Value("${aws_access_key}")
	String awsAccessKey;
	@Value("${aws_secret_key}")
	String awsSecretKey;
	@Value("${s3.bucket}")
	String bucketName;
	@Value("${s3.region}")
	String bucketRegion;


	/** Serves the index.html template */
	@GetMapping("/")
	public String appEntryPoint() {
		return "index";
	}

	/** REST endpoint to obtain a presigned url to S3 */
	@GetMapping(value = "/signedUploadUrls", produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public PresignedUrlResponse getPresignedUrls(@RequestParam String fileName) throws Exception {
		PresignedUrlResponse response = PresignedUrlResponse.builder()
				.urlWithoutAcceleration(presignedUploadUrl(fileName, config -> config.accelerateModeEnabled(false)))
				.urlWithAcceleration(presignedUploadUrl(fileName, config -> config.accelerateModeEnabled(true)))
				.build();
		log.info("Response: {}", response);
		return response;
	}

	private PresignedUrlResponse.PresignedUrl presignedUploadUrl(String objectKey, Consumer<S3Configuration.Builder> s3Configuration) {
		S3Configuration.Builder builder = S3Configuration.builder();
		s3Configuration.accept(builder);
		S3Configuration configuration = builder.build();

		AwsCredentialsProvider credentialsProvider =
				StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey, awsSecretKey));

		S3Presigner s3Presigner =
				S3Presigner.builder()
						.credentialsProvider(credentialsProvider)
						.serviceConfiguration(configuration)
						.region(Region.of(bucketRegion))
						.build();

		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(x -> x.signatureDuration(Duration.ofMinutes(20))
				.putObjectRequest(r -> r.bucket(bucketName).key(objectKey)));

		// It is recommended to close the S3Presigner when it is done being used, because some credential
		// providers (e.g. if your AWS profile is configured to assume an STS role) require system resources
		// that need to be freed. If you are using one S3Presigner per application (as recommended), this
		// usually is not needed.
		s3Presigner.close();

		return PresignedUrlResponse.PresignedUrl.builder()
				.url(presignedRequest.url().toString())
				.method(presignedRequest.httpRequest().method().name())
				.headers(presignedRequest.httpRequest().headers())
				.expiration(presignedRequest.expiration())
				.build();
	}

	/** Simple Response Object */
	@lombok.Value
	@Builder
	public static class PresignedUrlResponse {
		private PresignedUrl urlWithoutAcceleration;
		private PresignedUrl urlWithAcceleration;

		@lombok.Value
		@Builder
		public static class PresignedUrl {
			private String url;
			private String method;
			private Map<String, List<String>> headers;
			private Instant expiration;
		}
	}
}
