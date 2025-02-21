import argparse
import glob
import os
import subprocess


# Read one input file
def read_file(path):
    with open(path, 'r') as file:
        return file.readlines()


def remove_potential_comment(line):
    if line.contains('--'):
        return line.split('--')[0].strip()
    else:
        return line


# Get the database creation statements
# CREATE DATABASE ...
# USE ...
# CREATE TABLE ...
# INSERT INTO ...
def get_create_stmts(lines):
    stmts = list()
    for line in lines:
        if line.startswith('--') or line.startswith('DROP'):
            continue
        else:
            line = line.replace('\n', '')
            stmts.append(line)
    return stmts


# Extract a query from a line
def extract_query(line):
    start = line.index('SELECT')
    end = line.index(';') + 1
    return line[start : end]


def run(query):
    mutable_binary = '/Users/tobiaskopp/mutable/build/debug_shared/bin/shell'
    command = mutable_binary
    timeout = 5000  # milliseconds

    print(query)
    print(command)



    #if isinstance(command, Sequence) and not isinstance(command, str) and not isinstance(command, bytes):
    #    command = list(filter(lambda elem : len(elem) > 0, command))    # remove whitespaces in command sequence
    process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                               cwd=os.getcwd())
    try:
        proc_out, proc_err = process.communicate(query.encode('latin-1'), timeout=timeout)
    except subprocess.TimeoutExpired:
        raise Exception(f'Query timed out after {timeout} seconds')
    finally:
        if process.poll() is None:          # if process is still alive
            process.terminate()             # try to shut down gracefully
            try:
                process.wait(timeout=1)     # give process 1 second to terminate
            except subprocess.TimeoutExpired:
                process.kill()              # kill if process did not terminate in time

    out: str = proc_out.decode('latin-1')
    err: str = proc_err.decode('latin-1')

    assert process.returncode is not None
    if process.returncode or len(err):
        raise Exception("Unexpected Error")

    return out



# Analyze the log of one file (one single database) -> one error
def analyze_file(path, args):
    if 'cur' in path:
        return
    if args.verbose:
        print(f'Analyzing {path}')

    lines = read_file(path)
    create_stmts = get_create_stmts(lines)

    if args.mode == 'norec':
        first_query = extract_query(lines[0])
        second_query = extract_query(lines[1])

        first_score = lines[0].split(' ')[-1]
        second_score = lines[1].split(' ')[-1]

        print(first_query)
        print(second_query)

        # TODO run queries
        first_query = ''.join(create_stmts).strip().replace('\n', ' ') + first_query + '\n'
        out = run(first_query)
        print(out)








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
        analyze_file(log, args)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Analyze log files and extract potential bugs')
    parser.add_argument('path', nargs='*', help='Directory path of multiple files or single file path')
    parser.add_argument('-m', '--mode', help='Mode [norec, tlp, pqs]', dest='mode')
    parser.add_argument('-v', '--verbose', help='Verbose output', dest='verbose', default=False, action='store_true')
    args = parser.parse_args()
    main(args)
