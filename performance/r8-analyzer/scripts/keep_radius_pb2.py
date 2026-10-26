"""Generated protocol buffer code for keep_radius.proto."""

from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import runtime_version as _runtime_version
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder

_runtime_version.ValidateProtobufRuntimeVersion(
    _runtime_version.Domain.PUBLIC, 6, 33, 4, '', 'keep_radius.proto'
)
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()
DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x11keep_radius.proto\x12&com.android.tools.r8.blastradius.proto"\x9f\x02\n\x13KeepRuleBlastRadius\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x0e\n\x06source\x18\x02'
    b' \x01(\t\x12\x16\n\x0econstraints_id\x18\x03 \x01(\x05\x12F\n\x06origin\x18\x04'
    b' \x01(\x0b26.com.android.tools.r8.blastradius.proto.TextFileOrigin\x12I\n\x0cblast_radius\x18\x05'
    b' \x01(\x0b23.com.android.tools.r8.blastradius.proto.BlastRadius\x12A\n\x04tags\x18\x06'
    b' \x03(\x0e23.com.android.tools.r8.blastradius.proto.KeepRuleTag"w\n\x0bBlastRadius\x12\x13\n\x0bsubsumed_by\x18\x01'
    b' \x03(\x05\x12\x1a\n\x12class_blast_radius\x18\x02'
    b' \x03(\x05\x12\x1a\n\x12field_blast_radius\x18\x03'
    b' \x03(\x05\x12\x1b\n\x13method_blast_radius\x18\x04'
    b' \x03(\x05"\x7f\n\x19GlobalKeepRuleBlastRadius\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x0e\n\x06source\x18\x02 \x01(\t\x12F\n\x06origin\x18\x03'
    b' \x01(\x0b26.com.android.tools.r8.blastradius.proto.TextFileOrigin"j\n\x0fKeepConstraints\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12K\n\x0bconstraints\x18\x02'
    b' \x03(\x0e26.com.android.tools.r8.blastradius.proto.KeepConstraint"`\n\rKeptClassInfo\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x1a\n\x12class_reference_id\x18\x02'
    b' \x01(\x05\x12\x16\n\x0efile_origin_id\x18\x03'
    b' \x01(\x05\x12\x0f\n\x07kept_by\x18\x04'
    b' \x03(\x05"`\n\rKeptFieldInfo\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x1a\n\x12field_reference_id\x18\x02'
    b' \x01(\x05\x12\x16\n\x0efile_origin_id\x18\x03'
    b' \x01(\x05\x12\x0f\n\x07kept_by\x18\x04'
    b' \x03(\x05"b\n\x0eKeptMethodInfo\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x1b\n\x13method_reference_id\x18\x02'
    b' \x01(\x05\x12\x16\n\x0efile_origin_id\x18\x03'
    b' \x01(\x05\x12\x0f\n\x07kept_by\x18\x04'
    b' \x03(\x05"a\n\x0eFieldReference\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x1a\n\x12class_reference_id\x18\x02'
    b' \x01(\x05\x12\x19\n\x11type_reference_id\x18\x03'
    b' \x01(\x05\x12\x0c\n\x04name\x18\x04'
    b' \x01(\t"c\n\x0fMethodReference\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x1a\n\x12class_reference_id\x18\x02'
    b' \x01(\x05\x12\x1a\n\x12proto_reference_id\x18\x03'
    b' \x01(\x05\x12\x0c\n\x04name\x18\x04'
    b' \x01(\t"K\n\x0eProtoReference\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x15\n\rparameters_id\x18\x02'
    b' \x01(\x05\x12\x16\n\x0ereturn_type_id\x18\x03'
    b' \x01(\x05"4\n\rTypeReference\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x17\n\x0fjava_descriptor\x18\x02'
    b' \x01(\t";\n\x11TypeReferenceList\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x1a\n\x12type_reference_ids\x18\x02'
    b' \x03(\x05"\x9f\x01\n\nFileOrigin\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x10\n\x08filename\x18\x02 \x01(\t\x12Q\n\x10maven_coordinate\x18\x03'
    b' \x01(\x0b27.com.android.tools.r8.blastradius.proto.MavenCoordinate\x12'
    b' \n\x18provided_by_build_system\x18\x04'
    b' \x01(\x08"I\n\x14ClassFileInJarOrigin\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x16\n\x0efile_origin_id\x18\x02'
    b' \x01(\x05\x12\r\n\x05entry\x18\x03'
    b' \x01(\t"T\n\x0eTextFileOrigin\x12\x16\n\x0efile_origin_id\x18\x01'
    b' \x01(\x05\x12\x13\n\x0bline_number\x18\x02'
    b' \x01(\x05\x12\x15\n\rcolumn_number\x18\x03'
    b' \x01(\x05"U\n\x0fMavenCoordinate\x12\n\n\x02id\x18\x01'
    b' \x01(\x05\x12\x10\n\x08group_id\x18\x02 \x01(\t\x12\x13\n\x0bartifact_id\x18\x03'
    b' \x01(\t\x12\x0f\n\x07version\x18\x04'
    b' \x01(\t"\x9a\x01\n\tBuildInfo\x12\x13\n\x0bclass_count\x18\x01'
    b' \x01(\x05\x12\x13\n\x0bfield_count\x18\x02'
    b' \x01(\x05\x12\x14\n\x0cmethod_count\x18\x03'
    b' \x01(\x05\x12\x18\n\x10live_class_count\x18\x04'
    b' \x01(\x05\x12\x18\n\x10live_field_count\x18\x05'
    b' \x01(\x05\x12\x19\n\x11live_method_count\x18\x06'
    b' \x01(\x05"\xd5\n\n\x14BlastRadiusContainer\x12M\n\x11file_origin_table\x18\x01'
    b' \x03(\x0b22.com.android.tools.r8.blastradius.proto.FileOrigin\x12d\n\x1eclass_file_in_jar_origin_table\x18\x02'
    b' \x03(\x0b2<.com.android.tools.r8.blastradius.proto.ClassFileInJarOrigin\x12W\n\x16maven_coordinate_table\x18\x03'
    b' \x03(\x0b27.com.android.tools.r8.blastradius.proto.MavenCoordinate\x12U\n\x15field_reference_table\x18\x04'
    b' \x03(\x0b26.com.android.tools.r8.blastradius.proto.FieldReference\x12W\n\x16method_reference_table\x18\x05'
    b' \x03(\x0b27.com.android.tools.r8.blastradius.proto.MethodReference\x12U\n\x15proto_reference_table\x18\x06'
    b' \x03(\x0b26.com.android.tools.r8.blastradius.proto.ProtoReference\x12S\n\x14type_reference_table\x18\x07'
    b' \x03(\x0b25.com.android.tools.r8.blastradius.proto.TypeReference\x12\\\n\x19type_reference_list_table\x18\x08'
    b' \x03(\x0b29.com.android.tools.r8.blastradius.proto.TypeReferenceList\x12T\n\x15kept_class_info_table\x18\t'
    b' \x03(\x0b25.com.android.tools.r8.blastradius.proto.KeptClassInfo\x12T\n\x15kept_field_info_table\x18\n'
    b' \x03(\x0b25.com.android.tools.r8.blastradius.proto.KeptFieldInfo\x12V\n\x16kept_method_info_table\x18\x0b'
    b' \x03(\x0b26.com.android.tools.r8.blastradius.proto.KeptMethodInfo\x12W\n\x16keep_constraints_table\x18\x0c'
    b' \x03(\x0b27.com.android.tools.r8.blastradius.proto.KeepConstraints\x12a\n\x1ckeep_rule_blast_radius_table\x18\r'
    b' \x03(\x0b2;.com.android.tools.r8.blastradius.proto.KeepRuleBlastRadius\x12n\n#global_keep_rule_blast_radius_table\x18\x0e'
    b' \x03(\x0b2A.com.android.tools.r8.blastradius.proto.GlobalKeepRuleBlastRadius\x12E\n\nbuild_info\x18\x0f'
    b' \x01(\x0b21.com.android.tools.r8.blastradius.proto.BuildInfo*\x1f\n\x0bKeepRuleTag\x12\x10\n\x0cPACKAGE_WIDE\x10\x00*H\n\x0eKeepConstraint\x12\x12\n\x0eDONT_OBFUSCATE\x10\x00\x12\x11\n\rDONT_OPTIMIZE\x10\x01\x12\x0f\n\x0bDONT_SHRINK\x10\x02BE\n&com.android.tools.r8.blastradius.protoB\x19KeepRuleBlastRadiusProtosP\x01b\x06proto3'
)

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(
    DESCRIPTOR, 'keep_radius_pb2', _globals
)
if not getattr(_descriptor, '_USE_C_DESCRIPTORS', False):
  setattr(_globals['DESCRIPTOR'], '_loaded_options', None)
  setattr(
      _globals['DESCRIPTOR'],
      '_serialized_options',
      b'\n&com.android.tools.r8.blastradius.protoB\031KeepRuleBlastRadiusProtosP\001',
  )
  for _name, (_start, _end) in (
      ('_KEEPRULETAG', (3332, 3363)),
      ('_KEEPCONSTRAINT', (3365, 3437)),
      ('_KEEPRULEBLASTRADIUS', (62, 349)),
      ('_BLASTRADIUS', (351, 470)),
      ('_GLOBALKEEPRULEBLASTRADIUS', (472, 599)),
      ('_KEEPCONSTRAINTS', (601, 707)),
      ('_KEPTCLASSINFO', (709, 805)),
      ('_KEPTFIELDINFO', (807, 903)),
      ('_KEPTMETHODINFO', (905, 1003)),
      ('_FIELDREFERENCE', (1005, 1102)),
      ('_METHODREFERENCE', (1104, 1203)),
      ('_PROTOREFERENCE', (1205, 1280)),
      ('_TYPEREFERENCE', (1282, 1334)),
      ('_TYPEREFERENCELIST', (1336, 1395)),
      ('_FILEORIGIN', (1398, 1557)),
      ('_CLASSFILEINJARORIGIN', (1559, 1632)),
      ('_TEXTFILEORIGIN', (1634, 1718)),
      ('_MAVENCOORDINATE', (1720, 1805)),
      ('_BUILDINFO', (1808, 1962)),
      ('_BLASTRADIUSCONTAINER', (1965, 3330)),
  ):
    setattr(_globals[_name], '_serialized_start', _start)
    setattr(_globals[_name], '_serialized_end', _end)
