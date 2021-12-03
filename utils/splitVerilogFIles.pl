#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Cwd qw(abs_path);
use File::Find;
use File::Basename;
use Term::ANSIColor qw(:constants);

$| = 1;

my ($source_verilog_filename, $target_dir, $prefix);

GetOptions (
    'file|f=s'       => \$source_verilog_filename,
    'dir_target|d=s' => \$target_dir,
    'prefix|p=s'     => \$prefix,
);

if (not defined($target_dir)) {
    $target_dir = dirname(abs_path($source_verilog_filename));
}

if (not defined($prefix)) {
    $prefix = "pure"
}

open(my $source_verilog_file, $source_verilog_filename) ||
    die(RED, "[ERROR] Could not open $source_verilog_filename: $!", RESET);

my (@split_module_name, $split_verilog_filename, $split_verilog_file);
while( my $line = <$source_verilog_file>)  {
    if ($line =~ /^module /) {
        @split_module_name = split(/^module |\(/, $line);
        $split_verilog_filename = $target_dir."/".$prefix."_".$split_module_name[1].".v";
        open($split_verilog_file, ">", $split_verilog_filename) ||
            die(RED, "[ERROR] Could not open $split_verilog_filename: $!", RESET);
    }
    print($split_verilog_file $line);
    if ($line =~ /^endmodule/) {
        close($split_verilog_file);
        print("[INFO] Split ", GREEN, "$split_module_name[1]",
            RESET, " into \"$split_verilog_filename\".\n");
    }
}

close($source_verilog_file);
