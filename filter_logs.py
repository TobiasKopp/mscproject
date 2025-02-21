import argparse
import os
import glob

OUTPUT_DIR = 'filtered_logs'

# Read one input file
def read_file(path):
    with open(path, 'r') as file:
        return file.readlines()


# Filter out log files that fail due to IO exception (not an actual bug)
def main(args):
    # Get all logfiles
    log_files = list()
    for path in sorted(set(args.path)):
        if os.path.isfile(path):  # path is one log file
            log_files.append(path)
        else:  # path is a directory containing multiple experiment files
            log_files.extend(glob.glob(os.path.join(path, '**', '[!_]*.log'), recursive=True))

    log_files = sorted(list(set(log_files)))
    for log in log_files:
        if 'cur' in log:
            continue
        lines = read_file(log)
        ignore = False
        for line in lines:
            if 'IOException: Stream closed' in line:
                ignore = True
                break
        if not ignore:
            out_file = os.path.join(OUTPUT_DIR, log.split("/")[-1])
            with open(out_file, 'w') as out:
                out.writelines(lines)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Analyze log files and extract potential bugs')
    parser.add_argument('path', nargs='*', help='Directory path of multiple files or single file path')
    parser.add_argument('-m', '--mode', help='Mode [norec, tlp, pqs]', dest='mode')
    parser.add_argument('-v', '--verbose', help='Verbose output', dest='verbose', default=False, action='store_true')
    args = parser.parse_args()
    main(args) 
