#!/usr/bin/perl -wT 
use strict; 
use CGI; 
use CGI::Carp qw (fatalsToBrowser); 
use File::Basename; 
use MIME::Base64;

$CGI::POST_MAX = 1024*5000;
my $safe_filename_characters = "a-zA-Z0-9_.-";
my $upload_dir = "/var/www/uploaded";
my $max_rate_interval = 20; #send an e-mail if 10 files are uploaded in this many seconds
my $cgiObj = new CGI; 
my $filename = $cgiObj -> param("uploadedfile");
my $error_message = "";
if ( !checkRate( $max_rate_interval, $upload_dir ) ) {$error_message = "Too Fast!";} #TODO replace die with code to send email.
else {
if (!$filename) {exit;}

my ( $name, $path, $extension ) = fileparse ( $filename, '\..*' ); #parse out any leading path
$filename = $name . $extension;
$filename =~ tr/ /_/;  #spaces to underscores
$filename =~ s/[^$safe_filename_characters]//g; #get rid of unwanted characters
if ( $filename =~ /^([$safe_filename_characters]+)$/ ) { $filename = $1; } else { die "Filename contains invalid characters"; }

my $uploaded_file = $cgiObj -> upload("uploadedfile");

if ( -e "$upload_dir/$filename") {die;}

open (UPLOADFILE, ">$upload_dir/$filename") or die "$!";
binmode UPLOADFILE;

while (<$uploaded_file>) {
	my $decoded = decode_base64($_);
	print UPLOADFILE $decoded;
}
close UPLOADFILE;

#make file unreadable without changing the mode back
my $mode = 0200;
chmod $mode, "$upload_dir/$filename";
}
print $cgiObj -> header();
print $filename . $error_message;


sub checkRate {

my $interval = $_[0];
my $upload_dir = $_[1];
my $retval = 1;
my $ratefile_name = "rate/rate.txt";
open RATEFILE , "<$upload_dir/$ratefile_name";
my @lines = <RATEFILE>;
close RATEFILE;
my $curr_time = time();
if (scalar(@lines) > 0) {

	my $min_time = shift @lines;
	if (scalar(@lines) < 9) {
		unshift @lines, $min_time;
	}
	$min_time = $min_time + 20;
	if ($curr_time < $min_time) {
		$retval = 0;
	}
}
push @lines, "$curr_time\n";
open RATEFILE , ">$upload_dir/$ratefile_name";
my $out = join("", @lines);
print RATEFILE $out;
close RATEFILE;
return $retval;
}
