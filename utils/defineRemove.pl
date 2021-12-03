#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Cwd qw(getcwd);
use Cwd qw(abs_path);
use File::Find;
use File::Basename;
use Term::ANSIColor qw(:constants);

my $root_dir = getcwd;
$| = 1;
print("[INFO] Current dir $root_dir\n");

my ($source_verilog_filename, $target_dir);

GetOptions (
    'file|f=s'     => \$source_verilog_filename,
    'dir_target|d=s' => \$target_dir,
);

if (not defined($target_dir)) {
    $target_dir = dirname(abs_path($source_verilog_filename));
}

my $target_verilog_filename = $target_dir."/modified_".basename($source_verilog_filename);

print("[INFO] Remove the Verilog file $source_verilog_filename and write into $target_verilog_filename.\n");

my (@in_definition, $def_depth, $cond_en, $is_def, $is_commend);

open(my $source_verilog_file, $source_verilog_filename) ||
    die(RED, "[ERROR] Could not open $source_verilog_filename: $!", RESET);
open(my $modified_verilog_file, ">", $target_verilog_filename) ||
    die(RED, "[ERROR] Could not open $target_verilog_filename: $!", RESET);

my (undef, undef, $source_parse_suffix) =
    fileparse($source_verilog_filename, (".v", ".sv", ".fir", ".cpp", ".vcd", ".dat"));
my $valid_suffix = ($source_parse_suffix eq ".v") || ($source_parse_suffix eq ".sv");

if ((not defined($source_verilog_filename)) || (not $valid_suffix)) {
    die(RED, "[ERROR] Please check the input filename, whose suffix should be '.v' or '.sv'.", RESET);
}

while( my $line = <$source_verilog_file>)  {
    $is_def = 0;
    $is_commend = 0;
    if ($line =~ /`ifdef/){
        push(@in_definition, 0);
        $is_def = 1;
    }
    if ($line =~ /`ifndef/){
        push(@in_definition, 1);
        $is_def = 1;
    }
    if ($line =~ /`else/) {
        $cond_en = 0;
        $is_def = 1;
    }
    if ($line =~ /`define/) {
        $is_def = 1;
    }
    if ($line =~ /`endif/) {
        pop(@in_definition);
        $is_def = 1;
    }
    if ($line =~ /^\/\//) {
        $is_commend = 1;
    }
    $def_depth = @in_definition;
    if (($def_depth == 0 || $cond_en) && (not $is_def) && (not $is_commend)) {
        print($modified_verilog_file $line);
    }
    if ($line =~ /`ifndef RANDOMIZE_GARBAGE_ASSIGN/) {
        $cond_en = 1;
    }
}

close($modified_verilog_file);
close($source_verilog_file);
print(GREEN, "[INFO] SUCCESS!", RESET);
print(" All definitions in $source_verilog_filename are removed,");
print("new Verilog codes are written into $target_verilog_filename\n");
print(BRIGHT_MAGENTA, "[WARNING] Please use `vimdiff` to check the modified Verilog file!\n", RESET);
