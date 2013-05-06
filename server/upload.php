<?php

$output_dir = "uploaded/";
$input_filename = basename($_FILES['uploadedfile']['name']);
$input_temp_location = $_FILES['uploadedfile']['tmp_name'];
$output_filename = $output_dir . $input_filename;
if(is_uploaded_file($input_temp_location)) {
	echo "1\n";
} else {
	echo "0\n";
}
if(move_uploaded_file($input_temp_location, $output_filename . ".encoded")) {
	echo "The file " . $input_filename . " has been uploaded";
	chmod ($output_filename, 0644);
	$encoded_file = fopen($output_filename . ".encoded",'r');
	$decoded_file = fopen($output_filename, 'w');
	stream_filter_append($encoded_file, 'convert.base64-decode');
	stream_copy_to_stream($encoded_file, $decoded_file);
	fclose($encoded_file);
	fclose($decoded_file);
} else {
	echo "Error uploading file\n";
	echo "filename: " . $input_filename . "\n";
	echo "output directory: " . $output_dir . "\n";
}
?>

