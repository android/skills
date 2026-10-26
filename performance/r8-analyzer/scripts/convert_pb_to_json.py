"""Converts R8 keep radius protobuf files into JSON format."""

import glob
import os
import sys
from google.protobuf import json_format
from google.protobuf import message
import keep_radius_pb2


def convert_pb_to_json(input_pb_path, output_json_path):
  """Reads an R8 BlastRadiusContainer protobuf and writes it as JSON."""
  if os.path.isdir(input_pb_path):
    pb_files = glob.glob(os.path.join(input_pb_path, '*.pb'))
    if not pb_files:
      print(
          f'Error: No .pb files found in directory {input_pb_path}',
          file=sys.stderr,
      )
      return False
    input_pb_path = max(pb_files, key=os.path.getmtime)

  bundle = keep_radius_pb2.BlastRadiusContainer()

  try:
    with open(input_pb_path, 'rb') as pb_file:
      binary_data = pb_file.read()
  except OSError as e:
    print(f'Error reading file {input_pb_path}: {e}', file=sys.stderr)
    return False

  try:
    bundle.ParseFromString(binary_data)
  except message.DecodeError as e:
    print(f'Error parsing protobuf: {e}', file=sys.stderr)
    return False

  try:
    json_string = json_format.MessageToJson(
        bundle,
        always_print_fields_with_no_presence=True,
        preserving_proto_field_name=True,
        indent=4,
    )
    out_dir = os.path.dirname(output_json_path)
    if out_dir:
      os.makedirs(out_dir, exist_ok=True)
    with open(output_json_path, 'w', encoding='utf-8') as json_file:
      json_file.write(json_string)
    return True
  except (OSError, ValueError, TypeError, json_format.Error) as e:
    print(f'Error writing JSON: {e}', file=sys.stderr)
    return False


def main():
  """Entry point for CLI execution."""
  input_pb = sys.argv[1] if len(sys.argv) > 1 else None
  if not input_pb:
    candidates = glob.glob('app/build/reports/r8/*.pb') + glob.glob(
        'tmp/r8analysis/*.pb'
    )
    candidates = [c for c in candidates if os.path.isfile(c)]
    if not candidates:
      print('Error: No .pb file found', file=sys.stderr)
      sys.exit(1)
    input_pb = max(candidates, key=os.path.getmtime)
  default_out = (
      'tmp/r8analysis/keepruleradius.json'
      if 'r8analysis' in input_pb
      else 'tmp/keepradius/keepruleradius.json'
  )
  output_json = sys.argv[2] if len(sys.argv) > 2 else default_out
  if not convert_pb_to_json(input_pb, output_json):
    sys.exit(1)


if __name__ == '__main__':
  main()
