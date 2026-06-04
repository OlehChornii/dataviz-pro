Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
$jars = @(
  'C:\Users\Helgi\.m2\repository\org\mockito\mockito-core\5.7.0\mockito-core-5.7.0.jar',
  'C:\Users\Helgi\.m2\repository\org\mockito\mockito-junit-jupiter\5.7.0\mockito-junit-jupiter-5.7.0.jar'
)
foreach ($jar in $jars) {
  Write-Output "==== $jar ===="
  if (Test-Path $jar) {
    try {
      $z = [System.IO.Compression.ZipFile]::OpenRead($jar)
      $entry = $z.Entries | Where-Object { $_.FullName -ieq 'META-INF/MANIFEST.MF' }
      if ($entry) {
        $s = $entry.Open()
        $sr = New-Object System.IO.StreamReader($s)
        $content = $sr.ReadToEnd()
        $sr.Close(); $s.Close(); $z.Dispose()
        Write-Output $content
      } else {
        $z.Dispose(); Write-Output 'no-manifest'
      }
    } catch {
      Write-Output "error: $_"
    }
  } else {
    Write-Output 'jar-not-found'
  }
}
