#include "desktoplinking.h"
#include "bridge.h"
#include "eventdispatcher.h"

#include <QCoreApplication>
#include <QDebug>
#include <QDesktopServices>

namespace {
struct RegisterQMLMetaType {
  RegisterQMLMetaType() { qRegisterMetaType<DesktopLinking *>(); }
} registerMetaType;
} // namespace

class DesktopLinkingPrivate {
public:
  Bridge *bridge = nullptr;
  double callbackId;
};

DesktopLinking::DesktopLinking(QObject *parent)
    : QObject(parent), d_ptr(new DesktopLinkingPrivate) {

  QCoreApplication::instance()->installEventFilter(this);
  connect(this, &DesktopLinking::urlOpened, this, &DesktopLinking::handleURL);
}

DesktopLinking::~DesktopLinking() {
}

void DesktopLinking::setBridge(Bridge *bridge) {
  Q_D(DesktopLinking);
  d->bridge = bridge;
}

QString DesktopLinking::moduleName() { return "DesktopLinking"; }

QList<ModuleMethod *> DesktopLinking::methodsToExport() {
  return QList<ModuleMethod *>{};
}

QVariantMap DesktopLinking::constantsToExport() { return QVariantMap(); }

void DesktopLinking::handleURL(const QString url) {
    Q_D(DesktopLinking);
    qDebug() << "call of DesktopLinking::handleURL with param path: " << url;
    d->bridge->invokePromiseCallback(d->callbackId, QVariantList{url});
}

void DesktopLinking::setCallback(double callbackId) {
    Q_D(DesktopLinking);
    d->callbackId = callbackId;
    qDebug() << "call of DesktopLinking::setCallback with param callbackId: " << callbackId;
}